package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录响应
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "绑定邮箱，未绑定为空")
    private String email;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "权限编码列表")
    private List<String> permissions;
}
