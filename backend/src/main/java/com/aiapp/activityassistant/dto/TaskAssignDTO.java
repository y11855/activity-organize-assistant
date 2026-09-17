package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务派发请求
 */
@Data
@Schema(description = "任务派发请求")
public class TaskAssignDTO {

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "负责人 ID")
    private Long assigneeId;

    @Schema(description = "负责人姓名")
    private String assigneeName;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;
}
