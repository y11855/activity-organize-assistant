package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：用户-角色关联表（user_role）的数据访问接口
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
