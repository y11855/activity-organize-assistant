package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：活动群聊消息表（chat_message）的数据访问接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
