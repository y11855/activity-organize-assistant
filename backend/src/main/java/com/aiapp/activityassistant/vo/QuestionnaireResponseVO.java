package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名人员明细 VO
 */
@Data
@Schema(description = "报名人员明细")
public class QuestionnaireResponseVO {

    private Long id;

    @Schema(description = "填写人 ID")
    private Long respondentId;

    @Schema(description = "填写人姓名")
    private String respondentName;

    @Schema(description = "回答内容（JSON）")
    private String answer;

    @Schema(description = "报名状态: 1已报名 2已取消")
    private Integer status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
}
