package com.aiapp.activityassistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * MyBatis-Plus 实体：用户-角色关联（对应表 user_role，联合主键，无逻辑删除）
 */
@Data
@TableName("user_role")
public class UserRole implements Serializable {

    private Long userId;

    private Long roleId;
}
