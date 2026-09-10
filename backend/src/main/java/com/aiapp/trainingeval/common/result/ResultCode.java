package com.aiapp.trainingeval.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务码 1xxx
    BUSINESS_ERROR(1000, "业务异常"),
    FILE_PARSE_ERROR(1001, "文件解析失败"),
    AI_CALL_ERROR(1002, "AI 调用失败"),
    AI_EVALUATE_FAIL(1003, "AI 评价失败"),
    FILE_TOO_LARGE(1004, "文件超出大小限制");

    private final Integer code;
    private final String message;
}
