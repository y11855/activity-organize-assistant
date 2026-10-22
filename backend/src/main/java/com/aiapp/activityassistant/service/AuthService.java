package com.aiapp.activityassistant.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.LoginDTO;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证业务：登录签发 token（存 Redis）、登出、token 解析
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 登录 token 在 Redis 中的 key 前缀（RedisService 会再补全局前缀 activity:） */
    private static final String TOKEN_KEY = "login:token:";
    /** token 有效期 24 小时，每次访问自动续期 */
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final UserService userService;
    private final RedisService redisService;
    private final LoginCryptoService loginCryptoService;

    /**
     * 登录：解密前端传来的密码密文 → 校验用户名密码 → 签发 token
     */
    public LoginVO login(LoginDTO dto) {
        // 前端用 RSA 公钥加密密码，这里先解密还原明文，再交给 BCrypt 校验
        String rawPassword = loginCryptoService.decrypt(dto.getPassword());
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(rawPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }
        String token = IdUtil.fastSimpleUUID();
        // value 存字符串，避免小数字 ID 反序列化成 Integer 导致类型转换问题
        redisService.set(TOKEN_KEY + token, String.valueOf(user.getId()), TOKEN_TTL);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    /**
     * 登出：删除 token
     */
    public void logout(String token) {
        if (token != null) {
            redisService.delete(TOKEN_KEY + token);
        }
    }

    /**
     * 解析 token → 用户 ID；无效或过期返回 null
     */
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

    /**
     * 获取当前登录用户信息
     */
    public LoginVO currentUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LoginVO vo = new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }
}
