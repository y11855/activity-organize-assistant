package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送聊天消息请求
 */
@Data
@Schema(description = "发送聊天消息")
public class ChatMessageDTO {

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过 500 字")
    private String content;
}
