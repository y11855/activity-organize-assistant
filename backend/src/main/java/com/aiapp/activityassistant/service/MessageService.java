package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 站内消息业务接口
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息给指定用户
     */
    void send(Long userId, String title, String content, Long activityId);

    /**
     * 我的消息列表（新到旧）
     */
    List<Message> listMine();

    /**
     * 我的未读消息数
     */
    long unreadCount();

    /**
     * 标记单条已读（仅本人消息）
     */
    void markRead(Long id);

    /**
     * 全部标记已读
     */
    void markAllRead();
}
