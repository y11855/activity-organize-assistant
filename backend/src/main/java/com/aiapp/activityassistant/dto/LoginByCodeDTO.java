package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证码登录请求
 */
@Data
@Schema(description = "验证码登录请求")
public class LoginByCodeDTO {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "短信验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;
}
