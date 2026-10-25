package com.aiapp.activityassistant.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等：同一用户在时间窗内重复提交相同请求时只放行一次。
 * 由 IdempotentInterceptor 基于 Redis 原子抢占实现。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 业务前缀，缺省取方法名 */
    String prefix() default "";

    /** 防重时间窗（秒） */
    int ttl() default 5;
}
