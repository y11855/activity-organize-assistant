package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
