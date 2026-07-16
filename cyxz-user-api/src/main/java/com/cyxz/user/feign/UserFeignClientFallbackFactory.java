package com.cyxz.user.feign;

import com.cyxz.common.base.Result;
import com.cyxz.user.vo.UserProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户服务 Feign 降级工厂
 * <p>当 cyxz-user 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 */
@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    /**
     * 创建用户服务 Feign 降级实例，各方法返回安全默认值（null/空 Map/失败 Result）。
     *
     * @param cause 降级原因
     * @return 降级后的 UserFeignClient 实现
     */
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("用户服务调用降级: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<Void> initDefaultProfile(Long userId, String username) {
                log.error("初始化用户资料降级（需人工补偿）: userId={}, username={}", userId, username);
                return Result.fail("用户服务不可用");
            }

            @Override
            public Result<UserProfileVO> getById(Long userId) {
                return Result.success((UserProfileVO) null);
            }

            @Override
            public Result<Map<Long, UserProfileVO>> batchGetByIds(List<Long> userIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
