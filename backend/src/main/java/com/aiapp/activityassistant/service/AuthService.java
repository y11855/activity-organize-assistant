package com.aiapp.activityassistant.service;

import cn.hutool.crypto.digest.BCrypt;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.EmailLoginDTO;
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
    private final CaptchaService captchaService;
    private final EmailCodeService emailCodeService;

    /**
     * 密码登录：先校验图形验证码 → 解密前端传来的密码密文 → 校验用户名密码 → 签发 token
     */
    public LoginVO login(LoginDTO dto) {
        // 图形验证码防暴力破解，校验后即失效
        captchaService.verifyImageCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
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
     * 邮箱验证码登录：校验邮箱验证码 → 按邮箱查用户 → 签发 token（无需密码）。
     * 未绑定邮箱的账号无法使用此方式登录。
     */
    public LoginVO loginByEmailCode(EmailLoginDTO dto) {
        String email = EmailCodeService.normalize(dto.getEmail());
        User user = userService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱未绑定账号，请先登录后绑定邮箱");
        }
        if (Objects.equals(0, user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        emailCodeService.verify(email, dto.getCode());
        return issueToken(user);
    }

    /**
     * 发送登录用邮箱验证码：邮箱必须已绑定账号。
     */
    public void sendLoginCode(String email) {
        String key = EmailCodeService.normalize(email);
        User user = userService.getByEmail(key);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱未绑定账号，请先登录后绑定邮箱");
        }
        if (Objects.equals(0, user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        emailCodeService.send(key);
    }

    /**
     * 发送绑定邮箱验证码：邮箱不能被其他账号占用。
     */
    public void sendBindCode(Long userId, String email) {
        String key = EmailCodeService.normalize(email);
        User bound = userService.getByEmail(key);
        if (bound != null && !bound.getId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱已被其他账号绑定");
        }
        User current = userService.getById(userId);
        if (current == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (current.getEmail() != null && EmailCodeService.normalize(current.getEmail()).equals(key)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱已绑定当前账号");
        }
        emailCodeService.send(key);
    }

    /**
     * 绑定邮箱：校验验证码与唯一性后写入当前账号。
     */
    public LoginVO bindEmail(Long userId, EmailLoginDTO dto) {
        String email = EmailCodeService.normalize(dto.getEmail());
        User bound = userService.getByEmail(email);
        if (bound != null && !bound.getId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱已被其他账号绑定");
        }
        emailCodeService.verify(email, dto.getCode());
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        user.setEmail(email);
        userService.updateById(user);
        return buildLoginVO(user, null);
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
        String email = dto.getEmail() == null || dto.getEmail().isBlank()
                ? null
                : EmailCodeService.normalize(dto.getEmail());
        if (email != null && userService.getByEmail(email) != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱已被其他账号使用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? username
                : dto.getNickname().trim());
        user.setEmail(email);
        user.setStatus(1);
        try {
            userService.save(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名账号/同一邮箱时由唯一索引兜底
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或邮箱已被占用");
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
        vo.setEmail(user.getEmail());
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
