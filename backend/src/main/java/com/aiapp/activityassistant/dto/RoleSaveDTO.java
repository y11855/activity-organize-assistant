package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建/编辑角色
 */
@Data
@Schema(description = "角色信息")
public class RoleSaveDTO {

    @Schema(description = "角色编码（创建后不可修改，小写字母开头，可含数字和下划线）")
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{2,31}$", message = "角色编码需为 3-32 位小写字母、数字或下划线，且以字母开头")
    private String code;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称最长 32 个字符")
    private String name;

    @Schema(description = "角色说明")
    @Size(max = 200, message = "角色说明最长 200 个字符")
    private String description;
}
