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

    @Schema(description = "问卷标题")
    private String title;

    @Schema(description = "问卷描述")
    private String description;

    @Schema(description = "题目配置")
    private String questions;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "报名人数")
    private Integer responseCount;

    private LocalDateTime createTime;
}
