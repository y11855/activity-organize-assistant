package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱验证码登录/绑定请求
 */
@Data
@Schema(description = "邮箱验证码登录/绑定请求")
public class EmailLoginDTO {

    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 120, message = "邮箱最长 120 个字符")
    private String email;

    @Schema(description = "邮箱验证码（6 位数字）")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
    private String code;
}
