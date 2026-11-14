package com.aiapp.activityassistant.common.mq;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务事件类型（Redis Stream 消息体中的 type 字段）
 */
@Getter
@AllArgsConstructor
public enum ActivityEventType {

    ACTIVITY_CREATED("创建活动"),
    ACTIVITY_PUBLISHED("发布活动"),
    ACTIVITY_CANCELLED("取消活动"),
    REGISTRATION_SUBMITTED("新报名"),
    TASK_COMPLETED("任务完成"),
    REMINDER_SENT("提醒已发送");

    private final String description;
}
