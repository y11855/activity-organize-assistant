package com.aiapp.activityassistant.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 工具调用：工具执行结果封装（成功标记 / 数据 / 错误信息）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    private boolean success;

    /** 返回数据 */
    private Object data;

    /** 错误信息 */
    private String errorMessage;

    public static ToolResult ok(Object data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult fail(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }
}
