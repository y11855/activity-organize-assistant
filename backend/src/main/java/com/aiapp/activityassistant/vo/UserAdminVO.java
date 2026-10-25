package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户列表项
 */
@Data
@Schema(description = "后台用户")
public class UserAdminVO {

    private Long id;

    private String username;

    private String nickname;

    /** 账号状态: 1启用 0禁用 */
    private Integer status;

    /** 角色 ID 列表 */
    private List<Long> roleIds;

    /** 角色编码列表 */
    private List<String> roleCodes;

    /** 角色名称列表 */
    private List<String> roleNames;

    private LocalDateTime createTime;
}
