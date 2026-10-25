package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @Schema(description = "用户名（4-20 位字母、数字或下划线）")
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
    private String username;

    @Schema(description = "密码（前端 RSA 加密后的密文，明文 6-20 位）")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "昵称，缺省时使用用户名")
    @Size(max = 32, message = "昵称最长 32 个字符")
    private String nickname;
}
