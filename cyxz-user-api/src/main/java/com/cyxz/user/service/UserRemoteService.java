package com.cyxz.user.service;

import com.cyxz.common.base.Result;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 用户远程服务封装
 * <p>统一封装对 cyxz-user 服务的 Feign 调用，处理 null 结果。
 * <p>异常降级由 {@link com.cyxz.user.feign.UserFeignClientFallbackFactory} 处理，
 * 调用失败时返回空集合/空 Map，不影响主流程。
 */
@Service
@RequiredArgsConstructor
public class UserRemoteService {

    private final UserFeignClient userFeignClient;

    /**
     * 批量查询用户信息
     *
     * @param userIds 用户 ID 集合
     * @return 用户 ID 到用户资料的映射 Map，失败返回空 Map
     */
    public Map<Long, UserProfileVO> batchGetByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<Map<Long, UserProfileVO>> result = userFeignClient.batchGetByIds(new ArrayList<>(userIds));
        return result != null && result.getData() != null ? result.getData() : Collections.emptyMap();
    }

    /**
     * 根据用户 ID 查询资料
     *
     * @param userId 用户 ID
     * @return 用户资料，失败返回 null
     */
    public UserProfileVO getById(Long userId) {
        if (userId == null) {
            return null;
        }
        Result<UserProfileVO> result = userFeignClient.getById(userId);
        return result != null && result.getData() != null ? result.getData() : null;
    }
}
