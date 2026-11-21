package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
@Schema(description = "聊天消息")
public class ChatMessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "发送人 ID")
    private Long senderId;

    @Schema(description = "发送人昵称")
    private String senderName;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
