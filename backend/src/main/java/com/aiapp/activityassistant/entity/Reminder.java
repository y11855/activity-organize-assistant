package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时提醒任务
 */
@Data
@TableName("reminder")
public class Reminder implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
