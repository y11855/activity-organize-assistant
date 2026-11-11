package com.aiapp.activityassistant.config.shiro;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Bearer 凭证：对应请求头 {@code Authorization: Bearer <token>}。
 * principal 与 credentials 均为 Redis 中的登录 token 字符串。
 */
public class BearerAuthenticationToken implements AuthenticationToken {

    private final String token;

    public BearerAuthenticationToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
