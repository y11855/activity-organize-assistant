package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 问卷编辑请求
 */
@Data
@Schema(description = "问卷编辑请求")
public class QuestionnaireSaveDTO {

    @Schema(description = "问卷标题")
    @NotBlank(message = "问卷标题不能为空")
    private String title;

    @Schema(description = "问卷描述")
    private String description;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "题目配置（JSON 数组）")
    private String questions;
}
