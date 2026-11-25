package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.RolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：角色-权限关联表（role_permission）的数据访问接口
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
