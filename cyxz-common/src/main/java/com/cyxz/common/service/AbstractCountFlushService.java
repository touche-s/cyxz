package com.cyxz.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 计数刷库服务抽象基类
 * <p>统一将 Redis Hash 中的增量刷入 MySQL 的逻辑：遍历 Hash 所有 field，逐条 update，
 * 成功后扣减对应增量防止并发丢失；失败时保留增量等下次重试。
 * <p>子类只需在具体刷库方法中调用 {@link #flushDelta} 并传入目标 Mapper 的更新回调。
 */
public abstract class AbstractCountFlushService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final StringRedisTemplate stringRedisTemplate;

    protected AbstractCountFlushService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 通用增量刷库逻辑
     * <p>遍历 Hash 所有 field，逐条 update，成功后扣减对应增量；失败时不删，下次定时任务继续重试。
     *
     * @param deltaKey Redis Hash key
     * @param updater  增量更新回调，接收实体 ID 和增量值
     * @return 成功刷入条数
     */
    protected final int flushDelta(String deltaKey, BiConsumer<Long, Integer> updater) {
        HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
        Map<Object, Object> deltas = hashOps.entries(deltaKey);
        if (deltas == null || deltas.isEmpty()) {
            return 0;
        }

        int success = 0;
        for (Map.Entry<Object, Object> entry : deltas.entrySet()) {
            String idStr = String.valueOf(entry.getKey());
            String deltaStr = String.valueOf(entry.getValue());
            try {
                Long entityId = Long.valueOf(idStr);
                int delta = Integer.parseInt(deltaStr);
                updater.accept(entityId, delta);
                Long remaining = hashOps.increment(deltaKey, idStr, -delta);
                if (remaining != null && remaining <= 0) {
                    hashOps.delete(deltaKey, idStr);
                }
                success++;
            } catch (NumberFormatException e) {
                log.warn("增量格式异常，跳过: key={}, id={}, delta={}", deltaKey, idStr, deltaStr);
                hashOps.delete(deltaKey, idStr);
            } catch (Exception e) {
                log.error("刷库失败，保留增量等下次重试: key={}, id={}, delta={}", deltaKey, idStr, deltaStr, e);
            }
        }
        if (success > 0) {
            log.info("刷库完成: key={}, 成功 {} 条", deltaKey, success);
        }
        return success;
    }
}
