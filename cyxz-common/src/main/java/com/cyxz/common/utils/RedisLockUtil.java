package com.cyxz.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis SETNX 的轻量分布式锁
 * <p>用于定时任务多实例部署时的互斥（如计数刷库、补偿重试），
 * 避免多副本同时执行导致重复刷库/重复消费。
 * <p>锁带过期时间兜底，避免持有者宕机后死锁；不支持重入。
 */
public final class RedisLockUtil {

    private RedisLockUtil() {
    }

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param template   Redis 客户端
     * @param key        锁 Key（建议带业务前缀与 namespace）
     * @param ttlSeconds 锁过期秒数，需大于任务最长执行时长
     * @return true-获取成功
     */
    public static boolean tryLock(StringRedisTemplate template, String key, long ttlSeconds) {
        return Boolean.TRUE.equals(template.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS));
    }

    /**
     * 释放锁（仅由持有者调用）
     *
     * @param template Redis 客户端
     * @param key      锁 Key
     */
    public static void unlock(StringRedisTemplate template, String key) {
        template.delete(key);
    }
}
