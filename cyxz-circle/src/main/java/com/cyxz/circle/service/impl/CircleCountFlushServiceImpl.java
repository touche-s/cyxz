package com.cyxz.circle.service.impl;

import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.circle.service.CircleCountFlushService;
import com.cyxz.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircleCountFlushServiceImpl implements CircleCountFlushService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CircleMapper circleMapper;

    @Override
    public int flushPostCounts() {
        return flushDelta(CacheKeyConstants.CIRCLE_POST_DELTA,
                (circleId, delta) -> {
                    if (delta == 0) return;
                    circleMapper.updatePostCount(circleId, delta);
                });
    }

    private int flushDelta(String deltaKey, DeltaUpdater updater) {
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
                Long circleId = Long.valueOf(idStr);
                int delta = Integer.parseInt(deltaStr);
                updater.apply(circleId, delta);
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

    @FunctionalInterface
    private interface DeltaUpdater {
        void apply(Long entityId, int delta);
    }
}
