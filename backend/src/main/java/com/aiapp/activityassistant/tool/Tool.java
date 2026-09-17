package com.aiapp.activityassistant.tool;

import java.util.Map;

/**
 * 工具调用统一接口
 * 每个工具实现幂等调用，避免重复操作（如重复发消息）
 */
public interface Tool {

    /**
     * 工具名称（唯一标识）
     */
    String name();

    /**
     * 执行工具调用
     *
     * @param params 调用参数
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> params);
}
