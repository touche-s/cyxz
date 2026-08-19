package com.cyxz.circle.feign.fallback;

import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.common.base.ErrorCode;
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
            /**
             * 权限校验不可"假成功"——圈子服务宕机时返回失败，
             * 由调用方根据 {@link Result#isSuccess()} 区分"服务降级"与"权限不足"。
             */
            @Override
            public Result<PublishableResult> checkPublishable(Long circleId, Long userId) {
                return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMsg());
            }

            /**
             * 圈子存在性校验不可"假成功"——圈子服务宕机时返回失败，
             * 由调用方根据 {@link Result#isSuccess()} 区分"服务降级"与"圈子不存在"。
             */
            @Override
            public Result<Boolean> exists(Long circleId) {
                return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMsg());
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
