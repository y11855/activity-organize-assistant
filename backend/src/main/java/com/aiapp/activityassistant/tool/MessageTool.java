package com.aiapp.activityassistant.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息工具：发送提醒消息
 */
@Component
public class MessageTool implements Tool {

    @Override
    public String name() {
        return "message";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        // TODO: 接入消息推送（如飞书机器人/短信/站内信）
        // params: targetUserId, targetUserName, title, content
        String targetUserName = String.valueOf(params.get("targetUserName"));
        String title = String.valueOf(params.get("title"));
        return ToolResult.ok(Map.of(
                "messageId", "msg_" + System.currentTimeMillis(),
                "sentTo", targetUserName,
                "title", title
        ));
    }
}
