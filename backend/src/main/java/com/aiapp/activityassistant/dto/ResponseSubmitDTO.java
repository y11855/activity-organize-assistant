package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 问卷答卷提交请求
 */
@Data
@Schema(description = "问卷答卷提交请求")
public class ResponseSubmitDTO {

    @Schema(description = "回答内容（JSON）")
    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
