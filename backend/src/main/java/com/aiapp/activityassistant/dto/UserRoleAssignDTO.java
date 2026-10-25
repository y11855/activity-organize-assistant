package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 后台给用户分配角色
 */
@Data
@Schema(description = "分配用户角色")
public class UserRoleAssignDTO {

    @Schema(description = "角色 ID 列表（全量替换）")
    @NotEmpty(message = "请至少选择一个角色")
    private List<Long> roleIds;
}
