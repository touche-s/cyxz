package com.cyxz.circle.feign.fallback;

import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnClass(FallbackFactory.class)
public class CircleFeignClientFallbackFactory extends AbstractFeignFallbackFactory<CircleFeignClient> {

    @Override
    protected String serviceName() {
        return "圈子服务";
    }

    @Override
    protected CircleFeignClient createFallback(Throwable cause) {
        return new CircleFeignClient() {
            @Override
            public Result<PublishableResult> checkPublishable(Long circleId, Long userId) {
                PublishableResult fallback = new PublishableResult();
                return Result.success(fallback);
            }

            @Override
            public Result<Map<Long, String>> batchGetNames(Set<Long> circleIds) {
                return Result.success(Collections.emptyMap());
            }

            @Override
            public Result<Map<Long, String>> batchGetSectionNames(Set<Long> sectionIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
