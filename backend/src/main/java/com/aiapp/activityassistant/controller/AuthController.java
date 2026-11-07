package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.EmailCodeDTO;
import com.aiapp.activityassistant.dto.EmailLoginDTO;
import com.aiapp.activityassistant.dto.LoginDTO;
import com.aiapp.activityassistant.dto.RegisterDTO;
import com.aiapp.activityassistant.service.AuthService;
import com.aiapp.activityassistant.service.CaptchaService;
import com.aiapp.activityassistant.service.LoginCryptoService;
import com.aiapp.activityassistant.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginCryptoService loginCryptoService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取登录密码加密公钥")
    @GetMapping("/public-key")
    public Result<Map<String, String>> publicKey() {
        return Result.success(Map.of("publicKey", loginCryptoService.getPublicKey()));
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaService.CaptchaVO> captcha() {
        return Result.success(captchaService.generateImageCaptcha());
    }

    @Operation(summary = "发送登录邮箱验证码")
    @PostMapping("/email-code")
    public Result<Void> emailCode(@Valid @RequestBody EmailCodeDTO dto) {
        authService.sendLoginCode(dto.getEmail());
        return Result.success();
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login-by-email-code")
    public Result<LoginVO> loginByEmailCode(@Valid @RequestBody EmailLoginDTO dto) {
        return Result.success(authService.loginByEmailCode(dto));
    }

    @Operation(summary = "发送绑定邮箱验证码（需登录）")
    @PostMapping("/bind-email-code")
    public Result<Void> bindEmailCode(@Valid @RequestBody EmailCodeDTO dto) {
        authService.sendBindCode(UserContext.getCurrentUserId(), dto.getEmail());
        return Result.success();
    }

    @Operation(summary = "绑定邮箱（需登录）")
    @PostMapping("/bind-email")
    public Result<LoginVO> bindEmail(@Valid @RequestBody EmailLoginDTO dto) {
        return Result.success(authService.bindEmail(UserContext.getCurrentUserId(), dto));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/current")
    public Result<LoginVO> current() {
        return Result.success(authService.currentUser(UserContext.getCurrentUserId()));
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
