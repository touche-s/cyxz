package com.cyxz.user.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.user.vo.FollowUserVO;

import java.util.List;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注目标用户（幂等）
     * <p>不存在关注记录则插入 status=1，已存在且 status=0 则恢复关注，已关注则忽略。
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     */
    void follow(Long userId, Long targetUserId);

    /**
     * 取消关注目标用户（幂等）
     * <p>已关注则将 status 设为 0，不存在或已取消则忽略。
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     */
    void unfollow(Long userId, Long targetUserId);

    /**
     * 查询当前用户是否关注了目标用户
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     * @return true=已关注
     */
    boolean isFollowing(Long userId, Long targetUserId);

    /**
     * 统计当前用户的关注数
     *
     * @param userId 用户 ID
     * @return 关注数
     */
    int countFollowing(Long userId);

    /**
     * 统计当前用户的粉丝数
     *
     * @param userId 用户 ID
     * @return 粉丝数
     */
    int countFollowers(Long userId);

    /**
     * 分页查询当前用户的关注列表
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 关注用户列表（following 字段始终为 true）
     */
    PageResult<FollowUserVO> listFollowing(Long userId, int page, int size);

    /**
     * 分页查询当前用户的粉丝列表
     * <p>结果中包含 following 字段，标记当前用户是否已回关该粉丝。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 粉丝用户列表（含 following 回关状态）
     */
    PageResult<FollowUserVO> listFollowers(Long userId, int page, int size);

    /**
     * 统计今日新增粉丝数
     *
     * @param userId 用户 ID
     * @return 今日新增粉丝数
     */
    int countNewFollowers(Long userId);

    /**
     * 查询当前用户关注的用户 ID 列表（内部接口，供 post 服务拉取关注动态）
     *
     * @param userId 用户 ID
     * @return 关注的用户 ID 列表
     */
    List<Long> listFollowingUserIds(Long userId);
}
