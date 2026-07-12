package com.cyxz.comment.service;

import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.vo.CommentVO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param userId  当前登录用户 ID
     * @param request 创建评论请求
     * @return 新创建的评论 ID
     */
    Long createComment(Long userId, CreateCommentRequest request);

    /**
     * 删除评论（逻辑删除）
     * <p>将评论状态设为 0（已删除），校验归属权。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 分页查询帖子的评论列表
     * <p>仅返回顶级评论，子评论通过 children 字段嵌套返回。
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 评论视图列表
     */
    List<CommentVO> listComments(Long postId, int page, int size, Long currentUserId);

    /**
     * 点赞 / 取消点赞评论
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     * @return 操作后的点赞数
     */
    int toggleLike(Long userId, Long commentId);
}
