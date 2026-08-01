package com.cyxz.user.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.service.FollowService;
import com.cyxz.user.service.UserProfileService;
import com.cyxz.user.vo.FollowUserVO;
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
    private final FollowService followService;

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
                                @CurrentUser Long userId) {
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
    public Result<UserProfileVO> getMyProfile(@CurrentUser Long userId) {
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

    /**
     * 关注用户
     *
     * @param targetUserId 目标用户 ID
     * @param userId       当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PostMapping("/{targetUserId}/follow")
    public Result<Void> follow(@PathVariable("targetUserId") Long targetUserId,
                               @CurrentUser Long userId) {
        followService.follow(userId, targetUserId);
        return Result.success();
    }

    /**
     * 取消关注用户
     *
     * @param targetUserId 目标用户 ID
     * @param userId       当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{targetUserId}/follow")
    public Result<Void> unfollow(@PathVariable("targetUserId") Long targetUserId,
                                 @CurrentUser Long userId) {
        followService.unfollow(userId, targetUserId);
        return Result.success();
    }

    /**
     * 查询当前用户是否关注了目标用户
     *
     * @param targetUserId 目标用户 ID
     * @param userId       当前登录用户 ID（由 Gateway 注入）
     * @return 是否关注
     */
    @GetMapping("/{targetUserId}/is-following")
    public Result<Boolean> isFollowing(@PathVariable("targetUserId") Long targetUserId,
                                       @CurrentUser Long userId) {
        return Result.success(followService.isFollowing(userId, targetUserId));
    }

    /**
     * 查询两个用户是否互相关注
     * <p>前端 ProfilePage 私信按钮判断用。
     *
     * @param targetUserId 目标用户 ID
     * @param userId       当前登录用户 ID（由 Gateway 注入）
     * @return 是否互相关注
     */
    @GetMapping("/{targetUserId}/is-mutual-following")
    public Result<Boolean> isMutualFollowing(@PathVariable("targetUserId") Long targetUserId,
                                              @CurrentUser Long userId) {
        return Result.success(followService.isMutualFollowing(userId, targetUserId));
    }

    /**
     * 查询当前用户的关注列表
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码
     * @param size   每页条数
     * @return 关注用户列表
     */
    @GetMapping("/following")
    public Result<PageResult<FollowUserVO>> listFollowing(@CurrentUser Long userId,
                                                     @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                     @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size) {
        return Result.success(followService.listFollowing(userId, page, size));
    }

    /**
     * 查询当前用户的粉丝列表
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码
     * @param size   每页条数
     * @return 粉丝列表
     */
    @GetMapping("/followers")
    public Result<PageResult<FollowUserVO>> listFollowers(@CurrentUser Long userId,
                                                     @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                     @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size) {
        return Result.success(followService.listFollowers(userId, page, size));
    }

    /**
     * 查询当前用户的关注数和粉丝数
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 关注数和粉丝数
     */
    @GetMapping("/follow-stats")
    public Result<Map<String, Integer>> getFollowStats(@CurrentUser Long userId) {
        return Result.success(Map.of(
                "followingCount", followService.countFollowing(userId),
                "followerCount", followService.countFollowers(userId),
                "newFollowerCount", followService.countNewFollowers(userId)
        ));
    }

    /**
     * 查询关注用户 ID 列表（内部接口，供 post 服务拉取关注动态）
     */
    @GetMapping("/internal/following-ids")
    public Result<List<Long>> listFollowingUserIds(@RequestParam("userId") Long userId) {
        return Result.success(followService.listFollowingUserIds(userId));
    }
}
