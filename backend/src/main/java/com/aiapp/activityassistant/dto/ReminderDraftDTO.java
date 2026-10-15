package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒草稿（AI 生成待确认的提醒规则，确认发布后落库为定时提醒）
 */
@Data
@Schema(description = "提醒草稿")
public class ReminderDraftDTO {

    @Schema(description = "提醒标题")
    private String title;

    @Schema(description = "提醒内容")
    private String content;

    @Schema(description = "提醒目标用户 ID（为空表示提醒活动创建人）")
    private Long targetUserId;

    @Schema(description = "提醒目标用户名")
    private String targetUserName;

    @Schema(description = "触发时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTime;
}
