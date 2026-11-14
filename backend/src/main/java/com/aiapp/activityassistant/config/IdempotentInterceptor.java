package com.aiapp.activityassistant.config;

import cn.hutool.crypto.digest.DigestUtil;
import com.aiapp.activityassistant.common.annotation.Idempotent;
import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 幂等拦截器：拦截标注 @Idempotent 的接口。
 * key = idem:{prefix}:{userId}:{sha256(METHOD|URI|查询串|请求体摘要)}，通过 Redis SET NX EX 原子抢占。
 * 请求体摘要由 {@link CachedBodyFilter} 提前计算；无请求体时兼容 X-Request-Id 头（供外部重试复用同一 key）。
 * 相同用户 + 相同路径 + 相同参数在 ttl 内只放行一次，可拦住前端连续点击造成的重复提交。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private final RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (annotation == null) {
            return true;
        }

        Long userId = UserContext.getCurrentUserId();
        String prefix = annotation.prefix().isBlank()
                ? handlerMethod.getMethod().getName()
                : annotation.prefix();
        Object bodyDigest = request.getAttribute(CachedBodyFilter.BODY_DIGEST_ATTRIBUTE);
        String requestId = request.getHeader("X-Request-Id");
        String raw = request.getMethod() + "|" + request.getRequestURI() + "|"
                + (request.getQueryString() == null ? "" : request.getQueryString()) + "|"
                + (bodyDigest != null ? bodyDigest : (requestId == null ? "" : requestId));
        String key = "idem:" + prefix + ":" + userId + ":" + DigestUtil.sha256Hex(raw);

        boolean acquired = redisService.setIfAbsent(key, "1",
                Duration.ofSeconds(Math.max(1, annotation.ttl())));
        if (!acquired) {
            log.warn("[幂等] 重复请求已拦截, key={}", key);
            throw new BusinessException(ResultCode.FREQUENT_OPERATION);
        }
        return true;
    }
}
