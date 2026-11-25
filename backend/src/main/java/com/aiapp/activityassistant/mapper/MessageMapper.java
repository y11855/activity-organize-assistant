package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：站内消息表（message）的数据访问接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
