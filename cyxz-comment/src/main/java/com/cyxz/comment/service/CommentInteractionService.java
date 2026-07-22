package com.cyxz.comment.service;

/**
 * 评论互动服务接口
 * <p>管理评论点赞等互动操作，统一切换到 Redis 增量计数方案。
 */
public interface CommentInteractionService {

    /**
     * 点赞评论（幂等，并发安全）
     * <p>关系表 comment_like 照常写，计数通过 Redis Hash 增量记录。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void likeComment(Long userId, Long commentId);

    /**
     * 取消点赞评论（幂等，并发安全）
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void unlikeComment(Long userId, Long commentId);
}
