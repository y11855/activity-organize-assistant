package com.aiapp.activityassistant.tool;

import com.aiapp.activityassistant.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息工具：将提醒写入站内信（message 表），用户可在消息中心查看。
 * 后续可扩展为多渠道分发（飞书机器人 / 邮件 / 短信），站内信作为保底渠道。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTool implements Tool {

    private final MessageService messageService;

    @Override
    public String name() {
        return "message";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long targetUserId = toLong(params.get("targetUserId"));
        String targetUserName = String.valueOf(params.get("targetUserName"));
        String title = String.valueOf(params.get("title"));
        String content = String.valueOf(params.get("content"));
        Long activityId = toLong(params.get("activityId"));

        if (targetUserId == null) {
            return ToolResult.fail("目标用户 ID 缺失");
        }
        // 落站内信；标题前加【提醒】便于区分
        String fullTitle = "【提醒】" + title;
        messageService.send(targetUserId, fullTitle, content, activityId);
        log.info("[消息工具] 已发送站内信 -> userId={}, title={}", targetUserId, title);
        return ToolResult.ok(Map.of(
                "sentTo", targetUserName,
                "title", title
        ));
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
