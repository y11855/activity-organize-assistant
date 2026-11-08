package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码：通过邮箱验证码重置密码
 */
@Data
@Schema(description = "通过邮箱验证码重置密码")
public class ResetPasswordByEmailDTO {

    @Schema(description = "已绑定邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 120, message = "邮箱最长 120 个字符")
    private String email;

    @Schema(description = "邮箱验证码")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;

    @Schema(description = "新密码（RSA 密文）")
    @NotBlank(message = "新密码不能为空")
    private String password;
}
