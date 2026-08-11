package com.cyxz.user.feign;

import com.cyxz.common.base.Result;
import com.cyxz.user.vo.UserProfileVO;
import com.cyxz.user.feign.fallback.UserFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户服务 Feign 客户端
 * <p>供其他微服务调用 cyxz-user 的接口。
 */
@FeignClient(name = "cyxz-user", fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 创建默认用户资料
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    @PostMapping("/user/internal/profile/init/{userId}/{username}")
    Result<Void> initDefaultProfile(@PathVariable("userId") Long userId, @PathVariable("username") String username);

    /**
     * 批量查询用户资料（内部接口，供 post/comment 等服务调用）
     *
     * @param userIds 用户 ID 列表（最多 200 个）
     * @return userId → UserProfileVO 映射
     */
    @PostMapping("/user/internal/profile/batch")
    Result<Map<Long, UserProfileVO>> batchGetUserProfiles(@RequestBody List<Long> userIds);

    /**
     * 查询用户关注的用户 ID 列表（内部接口，供 post 服务拉取关注动态）
     *
     * @param userId 用户 ID
     * @return 关注的用户 ID 列表
     */
    @GetMapping("/user/internal/following-ids")
    Result<List<Long>> getFollowingUserIds(@RequestParam("userId") Long userId);

    /**
     * 查询两个用户是否互相关注（内部接口，供私信服务调用）
     *
     * @param userId       当前用户 ID
     * @param targetUserId 目标用户 ID
     * @return true=互相关注
     */
    @GetMapping("/user/internal/follow/mutual")
    Result<Boolean> isMutualFollowing(@RequestParam("userId") Long userId,
                                      @RequestParam("targetUserId") Long targetUserId);
}
