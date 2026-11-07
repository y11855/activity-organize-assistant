package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    private String nickname;

    /** 邮箱（用于验证码登录，可空） */
    private String email;

    /** 账号状态: 1启用 0禁用 */
    private Integer status;
}
