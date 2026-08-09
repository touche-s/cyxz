package com.cyxz.user.utils;

import com.cyxz.common.utils.FeignResults;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * UserFeignClient 调用工具
 */
public final class UserFeignHelper {

    private UserFeignHelper() {}

    /**
     * 批量查询用户资料，处理 Feign 降级结果
     *
     * @param client  UserFeignClient 实例
     * @param userIds 用户 ID 集合
     * @return userId → UserProfileVO 映射，降级时返回空 Map
     */
    public static Map<Long, UserProfileVO> batchGetUsers(UserFeignClient client, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return FeignResults.unwrapOrEmptyMap(client.batchGetUserProfiles(new ArrayList<>(userIds)));
    }
}
