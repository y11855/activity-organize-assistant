package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Reminder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：定时提醒表（reminder）的数据访问接口
 */
@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {
}
