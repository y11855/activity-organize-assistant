package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MyBatis-Plus 实体：活动群聊消息（对应表 chat_message）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    /** 所属活动 ID */
    private Long activityId;

    /** 发送人 ID */
    private Long senderId;

    /** 发送人昵称（冗余存储，避免拉历史记录时再查用户表） */
    private String senderName;

    /** 消息内容 */
    private String content;
}
