package com.aiapp.activityassistant.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;

/**
 * 验证码服务：图形验证码（防暴力破解）
 * 生成 base64 图片，答案存 Redis，2 分钟过期，校验后立即删除（一次性）。
 * 邮箱验证码见 {@link EmailCodeService}。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY = "captcha:img:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);

    private final RedisService redisService;

    /** 生成图形验证码，返回 key + base64 图片 */
    public CaptchaVO generateImageCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();
        String key = IdUtil.fastSimpleUUID();
        redisService.set(CAPTCHA_KEY + key, code.toLowerCase(), CAPTCHA_TTL);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(captcha.getImageBytes());
        return new CaptchaVO(key, base64);
    }

    /** 校验图形验证码（一次性消费） */
    public void verifyImageCaptcha(String key, String code) {
        if (key == null || code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入验证码");
        }
        String cached = (String) redisService.get(CAPTCHA_KEY + key);
        if (cached == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED, "验证码已过期，请刷新");
        }
        // 一次性：无论对错都删除，避免被反复尝试
        redisService.delete(CAPTCHA_KEY + key);
        if (!cached.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(ResultCode.CAPTCHA_INVALID, "验证码错误");
        }
    }

    /** 图形验证码返回值 */
    public record CaptchaVO(String key, String imageBase64) {}
}
