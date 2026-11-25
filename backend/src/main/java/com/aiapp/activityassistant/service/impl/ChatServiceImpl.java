package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.entity.ChatMessage;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.ChatMessageMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.ChatService;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.ChatMessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 业务实现：活动群聊领域的具体逻辑（权限校验、消息落库与历史查询）。
 * 实时推送由 {@code ChatSocketHandler} 负责，本类只处理持久化与权限校验。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /** 单次最多拉取条数，防止前端传大数拖库 */
    private static final int MAX_LIMIT = 100;

    private final ChatMessageMapper chatMessageMapper;
    private final ActivityService activityService;
    private final UserService userService;

    @Override
    public List<ChatMessageVO> list(Long activityId, int limit) {
        // 创建人 / 参与成员 / 有管理权限的人才能看聊天记录
        activityService.checkAccess(activityId);

        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getActivityId, activityId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + size));
        // 查询按时间倒序取最近 N 条，返回前翻正，保证页面从上到下是时间正序
        Collections.reverse(messages);
        return messages.stream().map(this::toVO).toList();
    }

    @Override
    public ChatMessageVO send(Long activityId, String content) {
        activityService.checkAccess(activityId);

        Long userId = UserContext.getCurrentUserId();
        User user = userService.getById(userId);

        ChatMessage message = new ChatMessage();
        message.setActivityId(activityId);
        message.setSenderId(userId);
        message.setSenderName(user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname());
        message.setContent(content);
        chatMessageMapper.insert(message);
        return toVO(message);
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
