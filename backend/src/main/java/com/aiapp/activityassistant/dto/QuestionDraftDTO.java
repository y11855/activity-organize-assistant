package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 问卷题目草稿（AI 生成待确认，可编辑）
 */
@Data
@Schema(description = "问卷题目草稿")
public class QuestionDraftDTO {

    @Schema(description = "题目")
    private String title;

    @Schema(description = "类型: single单选 multiple多选 text填空")
    private String type;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "选项（单选/多选）")
    private List<String> options;
}
