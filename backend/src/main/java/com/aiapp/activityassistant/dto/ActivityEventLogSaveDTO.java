package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动事件记录 DTO
 */
@Data
public class ActivityEventLogSaveDTO {

    @Schema(description = "事件类型: 1普通事件 2关键节点 3问题 4亮点", example = "1")
    @NotNull(message = "事件类型不能为空")
    private Integer eventType;

    @Schema(description = "事件内容")
    @NotBlank(message = "事件内容不能为空")
    private String content;

    @Schema(description = "发生时间（为空默认当前时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
}
