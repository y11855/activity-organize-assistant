package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 权限（后台）
 */
@Data
@Schema(description = "权限")
public class PermissionVO {

    private String code;

    private String name;

    /** 分组名称，后台按组渲染 */
    private String groupName;

    private Integer sort;
}
