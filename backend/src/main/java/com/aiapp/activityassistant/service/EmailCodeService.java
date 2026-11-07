package com.aiapp.activityassistant.service;

import cn.hutool.core.util.RandomUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 邮箱验证码服务：6 位数字，Redis 存 5 分钟，60 秒重发限制，校验后立即删除（一次性）。
 * 通过 SMTP 发送真实邮件，发件账号由 spring.mail.username/password 配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final String CODE_KEY = "captcha:email:";
    private static final String LIMIT_KEY = "captcha:email:limit:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration LIMIT_TTL = Duration.ofSeconds(60);

    private final RedisService redisService;
    private final JavaMailSender mailSender;

    /** 发件邮箱 */
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送验证码。
     */
    public void send(String email) {
        String key = normalize(email);
        if (!redisService.setIfAbsent(LIMIT_KEY + key, "1", LIMIT_TTL)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发送过于频繁，请 60 秒后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisService.set(CODE_KEY + key, code, CODE_TTL);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(key);
            message.setSubject("活动管家登录验证码");
            message.setText("您的验证码为 " + code + "，5 分钟内有效，请勿泄露给他人。");
            mailSender.send(message);
        } catch (Exception e) {
            // 发送失败时释放验证码与频率限制，允许用户立即重试
            redisService.delete(CODE_KEY + key);
            redisService.delete(LIMIT_KEY + key);
            log.error("邮箱验证码发送失败: {}", key, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /** 校验验证码（一次性消费） */
    public void verify(String email, String code) {
        String key = normalize(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入验证码");
        }
        String cached = (String) redisService.get(CODE_KEY + key);
        if (cached == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        redisService.delete(CODE_KEY + key);
        if (!cached.equals(code.trim())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }
    }

    /** 邮箱统一转小写去空格，避免大小写导致查不到账号 */
    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
