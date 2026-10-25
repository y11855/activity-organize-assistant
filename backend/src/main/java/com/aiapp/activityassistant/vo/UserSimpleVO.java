package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户简要信息（成员选择器用，不暴露密码等敏感字段）
 */
@Data
@Schema(description = "用户简要信息")
public class UserSimpleVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "账号状态：1启用 0禁用")
    private Integer status;
}
