package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.vo.ChatMessageVO;

import java.util.List;

/**
 * 业务接口：活动群聊领域的能力定义
 */
public interface ChatService {

    /** 拉取活动最近的聊天记录，按时间正序 */
    List<ChatMessageVO> list(Long activityId, int limit);

    /** 发送消息并落库 */
    ChatMessageVO send(Long activityId, String content);
}
