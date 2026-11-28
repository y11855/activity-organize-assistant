package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 请求 DTO：用户行为上报（问卷页停留时长、点赞）
 */
@Data
@Schema(description = "用户行为上报")
public class BehaviorReportDTO {

    @Schema(description = "问卷 ID")
    @NotNull(message = "问卷 ID 不能为空")
    private Long questionnaireId;

    @Schema(description = "行为类型: view停留 like点赞")
    @NotBlank(message = "行为类型不能为空")
    private String type;

    @Schema(description = "停留时长（秒），type=view 时有效")
    private Integer durationSeconds;
}
