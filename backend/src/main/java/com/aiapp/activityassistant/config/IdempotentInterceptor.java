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
 * key = idem:{prefix}:{userId}:{METHOD + URI + 查询串摘要}，通过 Redis SET NX EX 原子抢占。
 * 说明：preHandle 阶段无法拿到已反序列化的请求体，因此摘要只覆盖请求方法、路径与查询参数；
 * 对「同一路径同一用户」的重复提交已足够，创建类接口建议配合前端防抖使用。
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
        String raw = request.getMethod() + "|" + request.getRequestURI() + "|"
                + (request.getQueryString() == null ? "" : request.getQueryString());
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
