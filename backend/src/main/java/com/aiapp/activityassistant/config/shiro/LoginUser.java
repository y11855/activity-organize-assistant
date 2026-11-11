package com.aiapp.activityassistant.config.shiro;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Shiro 认证主体：认证通过后由 {@link UserRealm} 构建，
 * 过滤器再据此填充 {@code UserContext}，避免同一请求重复查库。
 */
@Data
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long id;

    private String username;

    private List<String> roles;

    private Set<String> permissions;
}
