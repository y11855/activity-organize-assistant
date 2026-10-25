package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {

    /** 权限编码，如 activity:view / system:user:manage */
    private String code;

    private String name;

    /** 分组名称（后台按组展示） */
    private String groupName;

    private Integer sort;
}
