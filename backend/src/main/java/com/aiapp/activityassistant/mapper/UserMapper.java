package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：用户表（user）的数据访问接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
