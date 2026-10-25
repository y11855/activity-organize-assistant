package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends BaseEntity {

    /** 角色编码（唯一，创建后不可修改） */
    private String code;

    private String name;

    private String description;

    /** 是否内置角色: 0否 1是（内置角色不可删除、编码不可改） */
    private Integer builtin;
}
