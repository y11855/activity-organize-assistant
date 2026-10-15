package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.entity.Message;
import com.aiapp.activityassistant.mapper.MessageMapper;
import com.aiapp.activityassistant.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内消息业务实现
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public void send(Long userId, String title, String content, Long activityId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setActivityId(activityId);
        message.setReadStatus(0);
        save(message);
    }

    @Override
    public List<Message> listMine() {
        return list(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, UserContext.getCurrentUserId())
                .orderByDesc(Message::getCreateTime));
    }

    @Override
    public long unreadCount() {
        return count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, UserContext.getCurrentUserId())
                .eq(Message::getReadStatus, 0));
    }

    @Override
    public void markRead(Long id) {
        Message message = getById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 只能操作自己的消息
        if (!message.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        message.setReadStatus(1);
        updateById(message);
    }

    @Override
    public void markAllRead() {
        update(new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, UserContext.getCurrentUserId())
                .eq(Message::getReadStatus, 0)
                .set(Message::getReadStatus, 1));
    }
}
