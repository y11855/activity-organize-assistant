package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 后台给角色分配权限
 */
@Data
@Schema(description = "分配角色权限")
public class RolePermissionAssignDTO {

    @Schema(description = "权限编码列表（全量替换，空列表表示清空）")
    @NotNull(message = "权限列表不能为 null")
    private List<String> permissionCodes;
}
