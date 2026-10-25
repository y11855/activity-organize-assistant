package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最长 100 个字符")
    private String title;

    @Schema(description = "任务描述")
    @Size(max = 500, message = "任务描述最长 500 个字符")
    private String description;

    @Schema(description = "负责人 ID")
    private Long assigneeId;

    @Schema(description = "负责人姓名")
    @Size(max = 32, message = "负责人姓名最长 32 个字符")
    private String assigneeName;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @Schema(description = "距当前的小时数（AI 生成用，换算为 deadline）")
    private Integer deadlineOffsetHours;
}
