package com.cyxz.post.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 帖子互动服务接口
 * <p>管理点赞、收藏、浏览等互动操作，统一切换到 Redis 增量计数方案。
 */
public interface PostInteractionService {

    /**
     * 点赞帖子（幂等，并发安全）
     * <p>关系表 post_like 照常写，计数通过 Redis Hash 增量记录。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void likePost(Long userId, Long postId);

    /**
     * 取消点赞帖子（幂等，并发安全）
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void unlikePost(Long userId, Long postId);

    /**
     * 收藏帖子（幂等，并发安全）
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void collectPost(Long userId, Long postId);

    /**
     * 取消收藏帖子（幂等，并发安全）
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void uncollectPost(Long userId, Long postId);

    /**
     * 记录浏览
     * <p>Redis 去重（30min 内同一用户/IP 只算一次），去重通过则 Hash 增量 +1。
     *
     * @param postId  帖子 ID
     * @param userId  当前登录用户 ID（可为 null，游客按 IP 去重）
     * @param request HTTP 请求（用于获取 IP）
     */
    void recordView(Long postId, Long userId, HttpServletRequest request);
}
