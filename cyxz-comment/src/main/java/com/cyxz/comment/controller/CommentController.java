package com.cyxz.comment.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.service.CommentInteractionService;
import com.cyxz.comment.service.CommentService;
import com.cyxz.comment.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentInteractionService commentInteractionService;

    /**
     * 发表评论
     *
     * @param request 创建评论请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 新创建的评论视图对象（含完整用户信息，前端可直接插入列表展示）
     */
    @PostMapping
    public Result<CommentVO> create(@Valid @RequestBody CreateCommentRequest request,
                               @CurrentUser Long userId) {
        CommentVO vo = commentService.createComment(userId, request);
        return Result.success("评论成功", vo);
    }

    /**
     * 删除评论（逻辑删除）
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@PathVariable("commentId") Long commentId,
                               @CurrentUser Long userId) {
        commentService.deleteComment(userId, commentId);
        return Result.success("删除成功");
    }

    /**
     * 分页查询帖子的评论列表（仅顶级评论）
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始，默认 1）
     * @param size          每页条数（默认 20）
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 评论列表（含嵌套子回复，子回复默认带第一页）
     */
    @GetMapping("/list")
    public Result<PageResult<CommentVO>> list(@RequestParam("postId") Long postId,
                                        @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                        @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size,
                                        @CurrentUser(required = false) Long currentUserId) {
        return Result.success(commentService.listComments(postId, page, size, currentUserId));
    }

    /**
     * 分页查询某条评论的子回复
     *
     * @param parentId      父评论 ID
     * @param page          页码（从 1 开始，默认 1）
     * @param size          每页条数（默认 5）
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 子回复列表
     */
    @GetMapping("/replies")
    public Result<PageResult<CommentVO>> replies(@RequestParam("parentId") Long parentId,
                                           @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                           @RequestParam(value = "size", defaultValue = PageConstants.SIZE_5_STR) int size,
                                           @CurrentUser(required = false) Long currentUserId) {
        return Result.success(commentService.listReplies(parentId, page, size, currentUserId));
    }

    /**
     * 点赞评论（幂等）
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PutMapping("/{commentId}/like")
    public Result<Void> like(@PathVariable("commentId") Long commentId,
                             @CurrentUser Long userId) {
        commentInteractionService.likeComment(userId, commentId);
        return Result.success();
    }

    /**
     * 取消点赞评论（幂等）
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{commentId}/like")
    public Result<Void> unlike(@PathVariable("commentId") Long commentId,
                               @CurrentUser Long userId) {
        commentInteractionService.unlikeComment(userId, commentId);
        return Result.success();
    }

    /**
     * 查询用户收到的评论列表（对用户帖子的评论）
     * <p>用于互动管理，查询当前用户所有帖子收到的评论。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码（从 1 开始，默认 1）
     * @param size   每页条数（默认 20）
     * @return 评论列表
     */
    @GetMapping("/received")
    public Result<PageResult<CommentVO>> received(@CurrentUser Long userId,
                                                  @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                  @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size) {
        return Result.success(commentService.listReceivedComments(userId, userId, page, size));
    }

    /**
     * 评论管理：分页查询当前用户自己帖子下的评论
     * <p>用于评论管理页，按创建时间倒序。不传 postId 查所有帖子，传 postId 只查指定帖子。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入，作为帖子作者筛选）
     * @param postId 帖子 ID（可选）
     * @param page   页码（从 1 开始，默认 1）
     * @param size   每页条数（默认 20）
     * @return 评论列表（含帖子标题、回复目标用户昵称）
     */
    @GetMapping("/manage")
    public Result<PageResult<CommentVO>> manage(@CurrentUser Long userId,
                                                 @RequestParam(value = "postId", required = false) Long postId,
                                                 @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                 @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size,
                                                 @RequestParam(value = "sortAsc", defaultValue = "false") boolean sortAsc) {
        return Result.success(commentService.listManagedComments(userId, postId, page, size, sortAsc));
    }

    /**
     * 删除帖子下的所有评论及评论点赞（内部接口）
     * <p>供 post 服务在彻底删除帖子时调用，级联清理关联数据。
     *
     * @param postId 帖子 ID
     * @return 操作结果
     */
    @DeleteMapping("/internal/post/{postId}")
    public Result<Void> deleteByPostId(@PathVariable("postId") Long postId) {
        commentService.deleteCommentsByPostId(postId);
        return Result.success();
    }

    /**
     * 查询今日新增评论数（内部接口）
     *
     * @param postAuthorId 帖子作者 ID
     * @return 今日新增评论数
     */
    @GetMapping("/internal/today")
    public Result<Integer> todayComments(@RequestParam("postAuthorId") Long postAuthorId) {
        return Result.success(commentService.countTodayComments(postAuthorId));
    }
}
