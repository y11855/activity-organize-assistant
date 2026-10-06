package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时提醒任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reminder")
public class Reminder extends BaseEntity {

    private Long activityId;

    /** 提醒标题 */
    private String title;

    /** 提醒内容 */
    private String content;

    /** 提醒目标用户 ID */
    private Long targetUserId;

    /** 提醒目标用户名 */
    private String targetUserName;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 状态: 0待触发 1已发送 2已取消 3发送失败 */
    private Integer status;

    /** 幂等键（避免重复发送） */
    private String idempotentKey;

    /** 重试次数 */
    private Integer retryCount;
}
