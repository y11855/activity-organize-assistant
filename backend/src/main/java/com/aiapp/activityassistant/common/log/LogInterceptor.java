package com.aiapp.activityassistant.common.log;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.config.CachedBodyFilter;
import com.aiapp.activityassistant.entity.SysLog;
import com.aiapp.activityassistant.service.LogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日志采集拦截器：记录每次接口调用的用户、路径、参数、响应、耗时与结果。
 *
 * <p>注册在 Shiro 过滤器之后、业务方法之前，因此能直接读取 {@link UserContext}。
 * 采集在请求完成时进行，写库走异步线程，任何异常都不会影响主流程。</p>
 *
 * <p>日志类型归类（每次请求只落一条，避免重复统计）：
 * 登录接口 → login；失败请求 → exception；后台管理 → audit；
 * 写操作 → operation；其余 → api。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogInterceptor implements HandlerInterceptor {

    /** 请求属性：请求开始时间 */
    private static final String START_TIME_ATTRIBUTE = "SYS_LOG_START_TIME";
    /** 慢请求阈值（毫秒） */
    private static final long SLOW_THRESHOLD = 1000L;
    /** 不需要采集的静态资源与文档路径 */
    private static final List<String> IGNORED_PREFIXES = List.of(
            "/uploads", "/doc.html", "/v3/", "/swagger", "/webjars", "/favicon", "/druid", "/error", "/ws");
    /** 写操作：额外归类为「操作日志」 */
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    /** 登录相关接口 */
    private static final Set<String> LOGIN_PATHS = Set.of("/auth/login", "/auth/login-by-email-code");
    /** 请求路径首段 → 业务模块中文名 */
    private static final Map<String, String> MODULE_NAMES = Map.ofEntries(
            Map.entry("auth", "认证"),
            Map.entry("activities", "活动"),
            Map.entry("tasks", "任务"),
            Map.entry("questionnaires", "问卷"),
            Map.entry("reminders", "提醒"),
            Map.entry("messages", "消息"),
            Map.entry("admin", "系统管理"),
            Map.entry("logs", "日志中心"),
            Map.entry("dashboard", "数据大屏"),
            Map.entry("files", "文件"),
            Map.entry("behavior", "行为采集"),
            Map.entry("chat", "聊天"));

    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        try {
            String path = resolvePath(request);
            if (!isLoggable(request, path)) {
                return;
            }
            SysLog sysLog = buildLog(request, response, handler, path, ex);
            logService.saveAsync(sysLog);
        } catch (Exception e) {
            // 旁路逻辑：采集失败只告警，不影响已完成的业务响应
            log.warn("采集系统日志失败: {}", e.getMessage());
        }
    }

    /** 组装日志实体 */
    private SysLog buildLog(HttpServletRequest request, HttpServletResponse response,
            Object handler, String path, Exception ex) {
        String method = request.getMethod();
        int status = response.getStatus();
        boolean loginRequest = LOGIN_PATHS.contains(path);
        boolean failed = status >= 400 || ex != null;

        SysLog sysLog = new SysLog();
        sysLog.setLogType(resolveLogType(path, method, loginRequest, failed));
        sysLog.setModule(resolveModule(path));
        sysLog.setOperation(resolveOperation(handler, path));
        sysLog.setMethod(method);
        sysLog.setRequestUri(path);
        sysLog.setStatusCode(status);
        sysLog.setDuration(System.currentTimeMillis()
                - (long) request.getAttribute(START_TIME_ATTRIBUTE));
        sysLog.setSuccess(failed ? 0 : 1);
        sysLog.setIp(resolveIp(request));
        sysLog.setClient(LogTextUtils.truncate(request.getHeader("User-Agent"), 255));
        sysLog.setRequestParams(captureParams(request));

        String responseBody = (String) request.getAttribute(LogResponseAdvice.RESPONSE_BODY_ATTRIBUTE);
        sysLog.setResponseBody(LogTextUtils.truncate(responseBody));

        // 用户信息：已登录请求直接取上下文；登录请求从请求体/响应体补充
        sysLog.setUserId(UserContext.getCurrentUserIdOrNull());
        sysLog.setUserName(UserContext.getCurrentUserName());
        if (loginRequest) {
            fillLoginUser(sysLog, request, responseBody);
        }
        // 仅失败请求记录异常信息：优先取抛出的异常，其次取统一返回体中的 message
        sysLog.setErrorMsg(failed ? resolveErrorMsg(ex, responseBody) : null);
        return sysLog;
    }

    /** 日志类型：登录 > 异常 > 审计 > 操作 > 接口 */
    private String resolveLogType(String path, String method, boolean loginRequest, boolean failed) {
        if (loginRequest) {
            return "login";
        }
        if (failed) {
            return "exception";
        }
        if (path.startsWith("/admin") || ("/logs".equals(path) && "DELETE".equals(method))) {
            return "audit";
        }
        if (WRITE_METHODS.contains(method)) {
            return "operation";
        }
        return "api";
    }

    /** 登录日志补充操作人：失败取请求体用户名，成功可取响应体用户 ID */
    private void fillLoginUser(SysLog sysLog, HttpServletRequest request, String responseBody) {
        String requestBody = (String) request.getAttribute(CachedBodyFilter.BODY_CONTENT_ATTRIBUTE);
        if (!StringUtils.hasText(sysLog.getUserName())) {
            String name = readField(requestBody, "username");
            sysLog.setUserName(StringUtils.hasText(name) ? name : readField(requestBody, "email"));
        }
        if (sysLog.getUserId() == null) {
            String id = readNestedField(responseBody, "data", "id");
            if (StringUtils.hasText(id)) {
                try {
                    sysLog.setUserId(Long.valueOf(id));
                } catch (NumberFormatException ignored) {
                    // 响应结构异常时忽略，不影响日志入库
                }
            }
        }
    }

    /** 异常信息：异常对象优先，其次读取响应体 message */
    private String resolveErrorMsg(Exception ex, String responseBody) {
        if (ex != null) {
            return LogTextUtils.truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
        String message = readField(responseBody, "message");
        return StringUtils.hasText(message) ? LogTextUtils.truncate(message) : null;
    }

    /** 请求参数：查询串 + JSON 请求体，脱敏后截断 */
    private String captureParams(HttpServletRequest request) {
        StringBuilder params = new StringBuilder();
        if (StringUtils.hasText(request.getQueryString())) {
            params.append(request.getQueryString());
        }
        String body = (String) request.getAttribute(CachedBodyFilter.BODY_CONTENT_ATTRIBUTE);
        if (StringUtils.hasText(body)) {
            if (params.length() > 0) {
                params.append(" | ");
            }
            params.append(body);
        }
        return LogTextUtils.truncate(LogTextUtils.maskSensitive(params.toString()));
    }

    /** 操作描述：优先用控制器方法名，登录接口给中文说明 */
    private String resolveOperation(Object handler, String path) {
        if ("/auth/login".equals(path)) {
            return "用户登录";
        }
        if ("/auth/login-by-email-code".equals(path)) {
            return "邮箱验证码登录";
        }
        return handler instanceof HandlerMethod handlerMethod ? handlerMethod.getMethod().getName() : null;
    }

    /** 请求路径（去掉 context-path） */
    private String resolvePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
    }

    /** 是否采集：跳过静态资源、OPTIONS 预检 */
    private boolean isLoggable(HttpServletRequest request, String path) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return IGNORED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    /** 路径首段映射业务模块 */
    private String resolveModule(String path) {
        String[] segments = path.split("/");
        String first = segments.length > 1 ? segments[1] : "";
        return MODULE_NAMES.getOrDefault(first, "其他");
    }

    /** 客户端 IP：优先取代理转发头 */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** 读取 JSON 顶层字符串字段（解析失败返回 null） */
    private String readField(String json, String field) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json).path(field);
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /** 读取嵌套 JSON 字符串字段，例如 data.id */
    private String readNestedField(String json, String parent, String field) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json).path(parent).path(field);
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
