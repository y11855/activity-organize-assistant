package com.aiapp.trainingeval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 实训成果上传入参
 */
@Data
@Schema(description = "实训上传请求")
public class TrainingSaveDTO {

    @Schema(description = "实训标题")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "成果文件 URL")
    @NotBlank(message = "文件不能为空")
    private String fileUrl;

    @Schema(description = "学生 ID")
    private Long studentId;
}
