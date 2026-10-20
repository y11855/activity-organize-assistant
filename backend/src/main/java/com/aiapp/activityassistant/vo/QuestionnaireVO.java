package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "问卷")
public class QuestionnaireVO {

    private Long id;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "问卷类型: 1报名问卷 2活动反馈问卷")
    private Integer type;

    @Schema(description = "活动标题")
    private String activityTitle;

    @Schema(description = "问卷标题")
    private String title;

    @Schema(description = "问卷描述")
    private String description;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "题目配置")
    private String questions;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "报名人数")
    private Integer responseCount;

    private LocalDateTime createTime;
}
