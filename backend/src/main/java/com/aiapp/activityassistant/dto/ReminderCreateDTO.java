package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒创建请求
 */
@Data
@Schema(description = "提醒创建请求")
public class ReminderCreateDTO {

    @Schema(description = "活动 ID")
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    @Schema(description = "提醒标题")
    @NotBlank(message = "提醒标题不能为空")
    @Size(max = 64, message = "提醒标题最长 64 个字符")
    private String title;

    @Schema(description = "提醒内容")
    @Size(max = 500, message = "提醒内容最长 500 个字符")
    private String content;

    @Schema(description = "目标用户 ID")
    private Long targetUserId;

    @Schema(description = "目标用户名")
    @Size(max = 32, message = "目标用户名最长 32 个字符")
    private String targetUserName;

    @Schema(description = "触发时间")
    @NotNull(message = "触发时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTime;
}
