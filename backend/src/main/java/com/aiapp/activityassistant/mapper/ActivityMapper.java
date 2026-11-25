package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：活动表（activity）的数据访问接口
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}
