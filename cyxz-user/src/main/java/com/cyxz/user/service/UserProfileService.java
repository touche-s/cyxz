package com.cyxz.user.service;

import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.vo.UserProfileVO;

import java.util.List;
import java.util.Map;

/**
 * 用户资料服务接口
 */
public interface UserProfileService {

    /**
     * 根据用户 ID 查询资料
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    UserProfileVO getByUserId(Long userId);

    /**
     * 批量查询用户资料
     *
     * @param userIds 用户 ID 列表
     * @return userId → UserProfileVO 映射
     */
    Map<Long, UserProfileVO> batchGetUserProfiles(List<Long> userIds);

    /**
     * 修改用户资料
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求
     */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 创建默认资料（内部接口，注册时 auth 服务通过 Feign 调用）
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    void initDefaultProfile(Long userId, String username);

    /**
     * 查询当前登录用户的资料，查不到则兜底初始化
     * <p>用 X-User-Id 查，如果资料不存在则自动创建默认资料再返回。
     * 确保注册后进个人空间能立刻查到资料。
     *
     * @param userId 当前登录用户 ID（来自 X-User-Id Header）
     * @return 用户资料
     */
    UserProfileVO getOrInitMyProfile(Long userId);
}
