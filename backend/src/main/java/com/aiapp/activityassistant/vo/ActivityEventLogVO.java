package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动事件记录 VO
 */
@Data
@Schema(description = "活动事件记录")
public class ActivityEventLogVO {

    private Long id;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "事件类型: 1普通事件 2关键节点 3问题 4亮点")
    private Integer eventType;

    @Schema(description = "事件内容")
    private String content;

    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "记录人 ID")
    private Long recorderId;

    @Schema(description = "记录人姓名")
    private String recorderName;

    private LocalDateTime createTime;
}
