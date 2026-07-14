package com.cyxz.user.controller;

import com.cyxz.common.base.Result;
import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.service.UserProfileService;
import com.cyxz.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户资料控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    /**
     * 查询用户资料
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    @GetMapping("/{userId}")
    public Result<UserProfileVO> getById(@PathVariable("userId") Long userId) {
        return Result.success(profileService.getByUserId(userId));
    }

    /**
     * 修改用户资料
     *
     * @param request 更新请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PutMapping("/profile")
    public Result<Void> update(@Valid @RequestBody UpdateProfileRequest request,
                                @RequestHeader("X-User-Id") Long userId) {
        profileService.updateProfile(userId, request);
        return Result.success();
    }

    /**
     * 查询当前登录用户的资料（兜底初始化）
     * <p>从 X-User-Id 取当前登录用户 ID，查不到资料则自动创建默认资料。
     * 用于注册后进个人空间、个人中心等场景。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 当前用户资料
     */
    @GetMapping("/profile/me")
    public Result<UserProfileVO> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(profileService.getOrInitMyProfile(userId));
    }

    /**
     * 批量查询用户资料（内部接口，供 post/comment 等服务通过 Feign 调用）
     *
     * @param userIds 用户 ID 列表
     * @return userId → UserProfileVO 映射
     */
    @PostMapping("/internal/profile/batch")
    public Result<Map<Long, UserProfileVO>> batchGet(@RequestBody List<Long> userIds) {
        return Result.success(profileService.batchGetByUserIds(userIds));
    }

    /**
     * 创建默认资料（内部接口，注册时 auth 服务通过 Feign 调用）
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return 操作结果
     */
    @PostMapping("/internal/profile/init/{userId}/{username}")
    public Result<Void> initDefault(@PathVariable("userId") Long userId, @PathVariable("username") String username) {
        profileService.initDefaultProfile(userId, username);
        return Result.success();
    }
}
