package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.ActivityEventLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：活动事件日志表（activity_event_log）的数据访问接口
 */
@Mapper
public interface ActivityEventLogMapper extends BaseMapper<ActivityEventLog> {
}
