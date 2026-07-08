package com.cyxz.user.service;

import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.vo.UserProfileVO;

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
     * 修改用户资料
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求
     */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 创建默认资料（内部接口，注册时 auth 服务通过 Feign 调用）
     *
     * @param userId 用户 ID
     */
    void initDefaultProfile(Long userId);
}
