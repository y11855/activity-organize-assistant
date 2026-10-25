package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息（仅暴露前端需要的字段）
 */
@Data
@Schema(description = "站内消息")
public class MessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "接收人 ID")
    private Long userId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "关联活动 ID")
    private Long activityId;

    @Schema(description = "0未读 1已读")
    private Integer readStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
