package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的任务（个人分工视图）
 */
@Data
@Schema(description = "我的任务")
public class MyTaskVO {

    private Long id;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "活动标题")
    private String activityTitle;

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "任务描述（交付要求）")
    private String description;

    @Schema(description = "负责人 ID")
    private Long assigneeId;

    @Schema(description = "负责人姓名")
    private String assigneeName;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;

    @Schema(description = "状态: 0待开始 1进行中 2已完成 3已逾期")
    private Integer status;

    private LocalDateTime createTime;
}
