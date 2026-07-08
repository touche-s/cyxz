package com.cyxz.user.controller;

import com.cyxz.common.base.Result;
import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.service.UserProfileService;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<Void> update(@RequestBody UpdateProfileRequest request,
                                @RequestHeader("X-User-Id") Long userId) {
        profileService.updateProfile(userId, request);
        return Result.success();
    }

    /**
     * 创建默认资料（内部接口，注册时 auth 服务通过 Feign 调用）
     * <p>userId 由 Feign 通过 X-User-Id 请求头传递，不在 URL 上暴露。
     *
     * @param userId 用户 ID（从请求头获取）
     * @return 操作结果
     */
    @PostMapping("/internal/profile/init")
    public Result<Void> initDefault(@RequestHeader("X-User-Id") Long userId) {
        profileService.initDefaultProfile(userId);
        return Result.success();
    }
}
