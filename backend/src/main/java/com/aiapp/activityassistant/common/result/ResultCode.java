package com.aiapp.activityassistant.common.result;

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
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务码 1xxx
    BUSINESS_ERROR(1000, "业务异常"),
    AI_CALL_ERROR(1001, "AI 调用失败"),
    TOOL_CALL_ERROR(1002, "工具调用失败"),
    TOOL_RETRY_EXHAUSTED(1003, "工具调用重试次数已耗尽"),
    REMINDER_ALREADY_CANCELLED(1004, "提醒任务已取消"),
    ACTIVITY_NOT_OWNED(1005, "只能操作自己创建的活动"),
    FREQUENT_OPERATION(1006, "操作过于频繁，请稍后再试"),
    CHUNK_CHECKSUM_MISMATCH(1007, "分片校验失败，需要重传该分片"),
    CHUNK_NOT_COMPLETE(1008, "分片未全部上传，无法合并"),
    UPLOAD_SESSION_NOT_FOUND(1009, "上传会话不存在或已过期");

    private final Integer code;
    private final String message;
}
