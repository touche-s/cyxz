package com.cyxz.circle.feign.fallback;

import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.common.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CircleFeignClientFallbackFactory implements FallbackFactory<CircleFeignClient> {

    @Override
    public CircleFeignClient create(Throwable cause) {
        log.error("CircleFeignClient 调用失败，启用降级", cause);
        return new CircleFeignClient() {
            @Override
            public Result<Map<String, Object>> checkPublishable(Long circleId, Long userId) {
                Map<String, Object> fallback = Map.of(
                        "exists", false,
                        "enabled", false,
                        "joined", false,
                        "publishable", false
                );
                return Result.success(fallback);
            }

            @Override
            public Result<Map<Long, String>> batchGetNames(Set<Long> circleIds) {
                return Result.success(Collections.emptyMap());
            }

            @Override
            public Result<Boolean> validateSection(Long sectionId, Long circleId) {
                return Result.success(false);
            }

            @Override
            public Result<Map<Long, String>> batchGetSectionNames(Set<Long> sectionIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
