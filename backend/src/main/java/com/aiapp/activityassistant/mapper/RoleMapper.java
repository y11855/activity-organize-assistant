package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /** 查询用户拥有的角色编码 */
    @Select("""
            SELECT r.code
            FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            ORDER BY r.id
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 锁定除指定用户外、仍然启用且拥有 admin 角色的其他用户。
     * 用于防止并发操作把系统最后一名管理员移除/禁用/删除。
     */
    @Select("""
            SELECT u.id
            FROM user u
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE r.code = 'admin' AND r.deleted = 0
              AND u.deleted = 0 AND u.status = 1
              AND u.id <> #{excludeUserId}
            FOR UPDATE
            """)
    List<Long> selectOtherEnabledAdminIds(@Param("excludeUserId") Long excludeUserId);
}
