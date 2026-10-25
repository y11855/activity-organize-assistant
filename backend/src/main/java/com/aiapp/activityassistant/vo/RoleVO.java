package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色（后台）
 */
@Data
@Schema(description = "角色")
public class RoleVO {

    private Long id;

    private String code;

    private String name;

    private String description;

    /** 是否内置角色: 0否 1是 */
    private Integer builtin;

    /** 已分配的权限编码 */
    private List<String> permissionCodes;

    /** 使用该角色的用户数 */
    private Long userCount;
}
