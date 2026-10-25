package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台重置密码
 */
@Data
@Schema(description = "重置密码")
public class ResetPasswordDTO {

    @Schema(description = "新密码（前端 RSA 加密后的密文，明文 6-20 位）")
    @NotBlank(message = "新密码不能为空")
    private String password;
}
