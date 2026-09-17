package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒创建请求
 */
@Data
@Schema(description = "提醒创建请求")
public class ReminderCreateDTO {

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "提醒标题")
    private String title;

    @Schema(description = "提醒内容")
    private String content;

    @Schema(description = "目标用户 ID")
    private Long targetUserId;

    @Schema(description = "目标用户名")
    private String targetUserName;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;
}
