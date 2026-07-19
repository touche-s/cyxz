package com.cyxz.comment.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.vo.CommentVO;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param userId  当前登录用户 ID
     * @param request 创建评论请求
     * @return 新创建的评论视图对象（含用户信息，前端可直接插入列表展示）
     */
    CommentVO createComment(Long userId, CreateCommentRequest request);

    /**
     * 删除评论（逻辑删除）
     * <p>将评论状态设为 0（已删除），校验归属权。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 分页查询帖子的顶级评论列表
     * <p>仅对顶级评论做 SQL 分页，子评论默认携带第一页（3条），
     * 更多子评论通过 {@link #listReplies} 接口分页加载。
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 分页结果（仅顶级评论计入分页，含总条数）
     */
    PageResult<CommentVO> listComments(Long postId, int page, int size, Long currentUserId);

    /**
     * 分页查询某条评论的子回复
     *
     * @param parentId      父评论 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 分页结果
     */
    PageResult<CommentVO> listReplies(Long parentId, int page, int size, Long currentUserId);

    /**
     * 点赞评论（幂等）
     * <p>并发安全：尝试插入 status=1，冲突时捕获 DuplicateKeyException 重查真实状态。
     * 仅在真实状态变化时更新计数。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void likeComment(Long userId, Long commentId);

    /**
     * 取消点赞评论（幂等）
     * <p>使用条件更新，仅在 status=1 时改为 0，保证计数只减一次。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void unlikeComment(Long userId, Long commentId);

    /**
     * 查询用户收到的评论列表（对用户帖子的评论 + 回复我的评论）
     * <p>用于互动管理，查询当前用户所有帖子收到的评论和回复自己的评论，按创建时间倒序。
     *
     * @param userId        当前用户 ID
     * @param currentUserId 当前登录用户 ID（用于查点赞状态）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @return 分页结果
     */
    PageResult<CommentVO> listReceivedComments(Long userId, Long currentUserId, int page, int size);

    /**
     * 评论管理：查询当前用户自己帖子下的评论
     * <p>用于评论管理页，按创建时间倒序。传 postId 时只查该帖子下的评论。
     *
     * @param currentUserId 当前登录用户 ID（作为帖子作者筛选）
     * @param postId        帖子 ID（可选，null 表示查所有帖子）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @return 分页结果（含帖子标题、回复目标用户昵称）
     */
    PageResult<CommentVO> listManagedComments(Long currentUserId, Long postId, int page, int size, boolean sortAsc);

    /**
     * 删除指定帖子下的所有评论及评论点赞（物理删除，内部接口）
     * <p>用于帖子彻底删除时级联清理关联数据。
     *
     * @param postId 帖子 ID
     */
    void deleteCommentsByPostId(Long postId);
}
