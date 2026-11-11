package com.aiapp.activityassistant.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 系统状态码。
 *
 * <p><b>编码规范</b>：
 * <ul>
 *   <li>2xx/4xx/5xx 段：与 HTTP 语义严格对应，直接落到 HTTP 状态行；</li>
 *   <li>1xxx 段：业务错误，按模块分段，各自绑定一个语义相符的 HTTP 状态码
 *       （状态冲突 409、资源重复 409、依赖不可用 503、限流 429）；</li>
 *   <li>11xx 段：认证与账号类，<b>刻意映射为 400 而非 401</b>——登录失败若返回 401，
 *       前端 401 拦截器会把用户踢回登录页，而用户本来就在登录页，会造成自循环。</li>
 * </ul>
 *
 * <p>HTTP 状态码与业务码的映射只在本枚举内定义，业务代码不得自行指定状态码。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ===== 成功 =====
    SUCCESS(200, HttpStatus.OK, "操作成功"),

    // ===== 通用错误：与 HTTP 语义一一对应 =====
    PARAM_ERROR(400, HttpStatus.BAD_REQUEST, "参数错误"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "无权限访问该资源"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "资源不存在"),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),

    // ===== 通用业务 1000-1099 =====
    BUSINESS_ERROR(1000, HttpStatus.BAD_REQUEST, "业务异常"),
    STATE_CONFLICT(1010, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    RESOURCE_DUPLICATE(1011, HttpStatus.CONFLICT, "资源已存在"),
    PRECONDITION_FAILED(1012, HttpStatus.CONFLICT, "前置条件不满足"),

    // ===== AI 与工具 1001-1003：外部依赖不可用，用 503 表示「可重试」，区别于 500「我方 bug」 =====
    AI_CALL_ERROR(1001, HttpStatus.SERVICE_UNAVAILABLE, "AI 调用失败"),
    TOOL_CALL_ERROR(1002, HttpStatus.SERVICE_UNAVAILABLE, "工具调用失败"),
    TOOL_RETRY_EXHAUSTED(1003, HttpStatus.SERVICE_UNAVAILABLE, "工具调用重试次数已耗尽"),

    // ===== 提醒 1004、1013 =====
    REMINDER_ALREADY_CANCELLED(1004, HttpStatus.CONFLICT, "提醒任务已取消"),
    REMINDER_NOT_CANCELLABLE(1013, HttpStatus.CONFLICT, "提醒已发送或发送失败，无法取消"),

    // ===== 限流 1006：429 可配合 Retry-After 头，与 @Idempotent 的 ttl 呼应 =====
    FREQUENT_OPERATION(1006, HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),

    // ===== 文件分片 1007-1009 =====
    CHUNK_CHECKSUM_MISMATCH(1007, HttpStatus.BAD_REQUEST, "分片校验失败，需要重传该分片"),
    CHUNK_NOT_COMPLETE(1008, HttpStatus.CONFLICT, "分片未全部上传，无法合并"),
    UPLOAD_SESSION_NOT_FOUND(1009, HttpStatus.NOT_FOUND, "上传会话不存在或已过期"),

    // ===== 认证与账号 1100-1199：HTTP 一律 400，靠 code 区分具体原因 =====
    LOGIN_FAILED(1100, HttpStatus.BAD_REQUEST, "用户名或密码错误"),
    CAPTCHA_INVALID(1101, HttpStatus.BAD_REQUEST, "验证码错误"),
    CAPTCHA_EXPIRED(1102, HttpStatus.BAD_REQUEST, "验证码已过期"),
    EMAIL_NOT_BOUND(1103, HttpStatus.BAD_REQUEST, "该邮箱未绑定账号"),
    ;

    /** 业务码：响应体中的 code */
    private final Integer code;
    /** 对应的 HTTP 状态码：由异常处理器统一写入响应行 */
    private final HttpStatus httpStatus;
    /** 默认提示文案：可由 BusinessException 的自定义 message 覆盖 */
    private final String message;

    /**
     * 按业务码反查 HTTP 状态码。
     *
     * <p>未知码一律返回 500 而非 200——宁可报错，也不把失败伪装成成功。
     */
    public static HttpStatus toHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        for (ResultCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc.httpStatus;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
