package com.aiapp.activityassistant.config;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.service.AuthService;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * 认证拦截器：校验 Bearer token，加载用户角色/权限，填充 UserContext 与 SecurityContext。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final UserService userService;
    private final RbacService rbacService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = extractToken(request.getHeader("Authorization"));
        Long userId = authService.resolveUserId(token);
        if (userId == null) {
            return writeFail(response, ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        User user = userService.getById(userId);
        if (user == null) {
            return writeFail(response, ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        // 被禁用账号即使持有未过期 token 也直接拒绝
        if (Objects.equals(0, user.getStatus())) {
            return writeFail(response, ResultCode.UNAUTHORIZED.getCode(), "账号已被禁用");
        }

        List<String> roles = rbacService.listRoleCodes(userId);
        List<String> permissions = rbacService.listPermissionCodes(userId);
        UserContext.setCurrentUser(userId, roles, permissions);

        // 供 @PreAuthorize 使用的认证信息
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    private boolean writeFail(HttpServletResponse response, Integer code, String message) throws Exception {
        // 与全局异常风格一致：HTTP 200 + 业务码 401，前端拦截器统一处理跳转登录
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
        return false;
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
