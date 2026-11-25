package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 实体：活动事件日志（对应表 activity_event_log），记录活动期间的事件/关键节点/问题/亮点，作为 AI 复盘输入
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_event_log")
public class ActivityEventLog extends BaseEntity {

    private Long activityId;

    /** 事件类型: 1普通事件 2关键节点 3问题 4亮点 */
    private Integer eventType;

    /** 事件内容 */
    private String content;

    /** 发生时间 */
    private LocalDateTime occurredAt;

    /** 记录人 ID */
    private Long recorderId;
}
