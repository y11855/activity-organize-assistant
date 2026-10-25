package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-权限关联（联合主键，无逻辑删除）
 */
@Data
@TableName("role_permission")
public class RolePermission implements Serializable {

    private Long roleId;

    private Long permissionId;
}
