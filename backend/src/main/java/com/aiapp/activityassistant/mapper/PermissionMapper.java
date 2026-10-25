package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询用户通过所有角色获得的权限编码（去重） */
    @Select("""
            SELECT DISTINCT p.code
            FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN role r ON r.id = rp.role_id AND r.deleted = 0
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND p.deleted = 0
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
