package com.aiapp.activityassistant.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;

/**
 * 验证码服务：
 * 1. 图形验证码（防暴力破解）—— 生成 base64 图片，答案存 Redis，2 分钟过期，校验后立即删除（一次性）
 * 2. 短信验证码登录 —— 6 位数字，存 Redis 5 分钟，校验后立即删除（一次性）
 *    注意：sendSmsCode 演示用直接返回明文，生产环境应调用短信平台发送，不返回给前端
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY = "captcha:img:";
    private static final String SMS_KEY = "captcha:sms:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);
    private static final Duration SMS_TTL = Duration.ofMinutes(5);

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
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请刷新");
        }
        // 一次性：无论对错都删除，避免被反复尝试
        redisService.delete(CAPTCHA_KEY + key);
        if (!cached.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }
    }

    /** 发送短信验证码（演示：直接返回明文） */
    public String sendSmsCode(String username) {
        String code = RandomUtil.randomNumbers(6);
        redisService.set(SMS_KEY + username, code, SMS_TTL);
        return code;
    }

    /** 校验短信验证码（一次性消费） */
    public void verifySmsCode(String username, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入验证码");
        }
        String cached = (String) redisService.get(SMS_KEY + username);
        if (cached == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        redisService.delete(SMS_KEY + username);
        if (!cached.equals(code.trim())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }
    }

    /** 图形验证码返回值 */
    public record CaptchaVO(String key, String imageBase64) {}
}
