package com.aiapp.activityassistant.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 操作工具类：缓存读写、过期、原子抢占（幂等/分布式锁）
 */
@Component
@RequiredArgsConstructor
public class RedisService {

    /** 全局 key 前缀，避免和其他项目共用 Redis 时冲突 */
    public static final String KEY_PREFIX = "activity:";

    private final RedisTemplate<String, Object> redisTemplate;

    /** 写入缓存（不带过期时间） */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, value);
    }

    /** 写入缓存并设置过期时间 */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, value, timeout);
    }

    /** 读取缓存 */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + key);
    }

    /** 删除缓存，返回是否删除成功 */
    public Boolean delete(String key) {
        return redisTemplate.delete(KEY_PREFIX + key);
    }

    /** 是否存在 */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(KEY_PREFIX + key);
    }

    /** 设置过期时间 */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(KEY_PREFIX + key, timeout);
    }

    /**
     * 原子抢占（SET key value NX EX）：
     * 仅当 key 不存在时写入成功。用于幂等防重、分布式互斥。
     *
     * @return true=抢占成功；false=已被其他线程/节点占用
     */
    public boolean setIfAbsent(String key, String value, Duration timeout) {
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + key, value, timeout);
        return Boolean.TRUE.equals(ok);
    }

    /** 向集合添加元素 */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(KEY_PREFIX + key, values);
    }

    /** 读取集合全部元素 */
    public java.util.Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(KEY_PREFIX + key);
    }

    /** 从集合移除元素 */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(KEY_PREFIX + key, values);
    }

    /** 向列表左侧插入并裁剪，仅保留最近 maxSize 条（大屏实时动态用） */
    public void pushCapped(String key, Object value, int maxSize) {
        redisTemplate.opsForList().leftPush(KEY_PREFIX + key, value);
        redisTemplate.opsForList().trim(KEY_PREFIX + key, 0, maxSize - 1L);
    }

    /** 读取列表区间，start=0 表示最新一条 */
    public java.util.List<Object> range(String key, long start, long end) {
        java.util.List<Object> list = redisTemplate.opsForList().range(KEY_PREFIX + key, start, end);
        return list == null ? java.util.Collections.emptyList() : list;
    }
}
