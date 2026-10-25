package com.aiapp.activityassistant.service;

import cn.hutool.core.util.IdUtil;
import com.aiapp.activityassistant.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 登录 token 的签发、解析与吊销（Redis 存储）。
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    /** 登录 token 在 Redis 中的 key 前缀（RedisService 会再补全局前缀 activity:） */
    private static final String TOKEN_KEY = "login:token:";
    /** 用户维度的 token 集合，用于重置密码/禁用/删除时一次性吊销 */
    private static final String USER_TOKENS_KEY = "login:user-tokens:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final RedisService redisService;

    /** 签发 token，并记录到用户 token 集合 */
    public String issue(Long userId) {
        String token = IdUtil.fastSimpleUUID();
        // value 存字符串，避免小数字 ID 反序列化成 Integer 导致类型转换问题
        redisService.set(TOKEN_KEY + token, String.valueOf(userId), TOKEN_TTL);
        redisService.sAdd(USER_TOKENS_KEY + userId, token);
        redisService.expire(USER_TOKENS_KEY + userId, TOKEN_TTL);
        return token;
    }

    /** 解析 token → 用户 ID；无效或过期返回 null */
    public Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Object value = redisService.get(TOKEN_KEY + token);
        if (value == null) {
            return null;
        }
        // 命中后续期，保持活跃用户不过期
        redisService.expire(TOKEN_KEY + token, TOKEN_TTL);
        return Long.valueOf(String.valueOf(value));
    }

    /** 吊销单个 token */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Object value = redisService.get(TOKEN_KEY + token);
        redisService.delete(TOKEN_KEY + token);
        if (value != null) {
            redisService.sRemove(USER_TOKENS_KEY + String.valueOf(value), token);
        }
    }

    /** 吊销某个用户的全部 token（重置密码、禁用、删除账号时调用） */
    public void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        Set<Object> tokens = redisService.sMembers(USER_TOKENS_KEY + userId);
        if (tokens != null) {
            for (Object token : tokens) {
                redisService.delete(TOKEN_KEY + String.valueOf(token));
            }
        }
        redisService.delete(USER_TOKENS_KEY + userId);
    }
}
