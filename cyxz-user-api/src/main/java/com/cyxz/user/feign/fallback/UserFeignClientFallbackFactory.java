package com.cyxz.user.feign.fallback;

import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户服务 Feign 降级工厂
 * <p>当 cyxz-user 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 * <p>使用 {@code @ConditionalOnClass} 确保仅在有 openfeign 的消费方服务中注册。
 */
@Component
@ConditionalOnClass(FallbackFactory.class)
public class UserFeignClientFallbackFactory extends AbstractFeignFallbackFactory<UserFeignClient> {

    @Override
    protected String serviceName() {
        return "用户服务";
    }

    @Override
    protected UserFeignClient createFallback(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public Result<Void> initDefaultProfile(Long userId, String username) {
                log.error("初始化用户资料降级（需人工补偿）: userId={}, username={}", userId, username);
                return Result.fail("用户服务不可用");
            }

            @Override
            public Result<Map<Long, UserProfileVO>> batchGetByIds(List<Long> userIds) {
                return Result.success(Collections.emptyMap());
            }

            @Override
            public Result<List<Long>> getFollowingUserIds(Long userId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Boolean> isMutualFollowing(Long userId, Long targetUserId) {
                return Result.success(false);
            }
        };
    }
}
