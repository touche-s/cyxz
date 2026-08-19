package com.cyxz.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis SETNX 的轻量分布式锁
 * <p>用于定时任务多实例部署时的互斥（如计数刷库、补偿重试），
 * 避免多副本同时执行导致重复刷库/重复消费。
 * <p>锁带过期时间兜底，避免持有者宕机后死锁；不支持重入。
 * <p>释放锁时携带持有者 token，仅持有者本人能删除，防止锁过期后被其他实例
 * 接管、原持有者误删他人锁导致互斥失效。
 */
public final class RedisLockUtil {

    /** 释放锁 Lua 脚本：仅当 value 与持有者 token 一致时才删除，保证"谁持有谁释放" */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private RedisLockUtil() {
    }

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param template   Redis 客户端
     * @param key        锁 Key（建议带业务前缀与 namespace）
     * @param ttlSeconds 锁过期秒数，需大于任务最长执行时长
     * @return 获取成功返回持有者 token（释放锁时需回传），失败返回 null
     */
    public static String tryLock(StringRedisTemplate template, String key, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = template.opsForValue().setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    /**
     * 释放锁（仅由持有者调用）
     * <p>通过 Lua 比对持有者 token，避免锁已过期被其他实例接管后误删他人锁。
     *
     * @param template Redis 客户端
     * @param key      锁 Key
     * @param token    tryLock 返回的持有者 token
     */
    public static void unlock(StringRedisTemplate template, String key, String token) {
        if (token == null) {
            return;
        }
        template.execute(UNLOCK_SCRIPT, List.of(key), token);
    }
}
