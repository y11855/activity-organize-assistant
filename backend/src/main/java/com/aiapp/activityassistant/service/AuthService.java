package com.aiapp.activityassistant.service;

import cn.hutool.crypto.digest.BCrypt;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.LoginDTO;
import com.aiapp.activityassistant.dto.RegisterDTO;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 认证业务：登录签发 token、注册、登出、当前用户信息
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 注册用户默认授予的角色编码 */
    private static final String DEFAULT_ROLE_CODE = "member";

    private final UserService userService;
    private final LoginCryptoService loginCryptoService;
    private final TokenService tokenService;
    private final RbacService rbacService;

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
        if (Objects.equals(0, user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return issueToken(user);
    }

    /**
     * 注册：解密密码密文 → 校验规则与用户名唯一性 → BCrypt 入库 → 授予默认角色 → 直接签发 token
     */
    public LoginVO register(RegisterDTO dto) {
        // 与登录一致：密码在传输过程中始终是 RSA 密文
        String rawPassword = loginCryptoService.decrypt(dto.getPassword());
        if (rawPassword.length() < 6 || rawPassword.length() > 20) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码长度需为 6-20 位");
        }
        String username = dto.getUsername().trim();
        if (userService.getByUsername(username) != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? username
                : dto.getNickname().trim());
        user.setStatus(1);
        try {
            userService.save(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名账号时由唯一索引兜底
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已被占用");
        }
        rbacService.addUserRoleByCode(user.getId(), DEFAULT_ROLE_CODE);
        return issueToken(user);
    }

    /**
     * 签发 token 并组装登录响应
     */
    private LoginVO issueToken(User user) {
        String token = tokenService.issue(user.getId());
        return buildLoginVO(user, token);
    }

    private LoginVO buildLoginVO(User user, String token) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoles(rbacService.listRoleCodes(user.getId()));
        vo.setRoleNames(rbacService.listRoleNames(user.getId()));
        vo.setPermissions(rbacService.listPermissionCodes(user.getId()));
        return vo;
    }

    /**
     * 登出：吊销当前 token
     */
    public void logout(String token) {
        tokenService.revoke(token);
    }

    /**
     * 解析 token → 用户 ID；无效或过期返回 null
     */
    public Long resolveUserId(String token) {
        return tokenService.resolveUserId(token);
    }

    /**
     * 获取当前登录用户信息
     */
    public LoginVO currentUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return buildLoginVO(user, null);
    }
}
