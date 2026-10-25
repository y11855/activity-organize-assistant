package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联（联合主键，无逻辑删除）
 */
@Data
@TableName("user_role")
public class UserRole implements Serializable {

    private Long userId;

    private Long roleId;
}
