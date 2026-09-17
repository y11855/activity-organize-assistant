package com.aiapp.activityassistant.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 日历工具：将活动写入日历
 */
@Component
public class CalendarTool implements Tool {

    @Override
    public String name() {
        return "calendar";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        // TODO: 接入日历 API（如飞书/企业微信日历）
        // params: title, startTime, endTime, location, attendees
        return ToolResult.ok(Map.of(
                "eventId", "calendar_" + System.currentTimeMillis(),
                "status", "created"
        ));
    }
}
