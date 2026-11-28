package com.aiapp.activityassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求 DTO：更新定时提醒（创建者自定义提醒时间/标题/内容）。
 * 字段均可选，非空才更新，允许只改触发时间而保留标题内容。
 */
@Data
@Schema(description = "提醒更新请求")
public class ReminderUpdateDTO {

    @Schema(description = "提醒标题")
    @Size(max = 64, message = "提醒标题最长 64 个字符")
    private String title;

    @Schema(description = "提醒内容")
    @Size(max = 500, message = "提醒内容最长 500 个字符")
    private String content;

    @Schema(description = "触发时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTime;
}
