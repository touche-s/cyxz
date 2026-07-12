package com.cyxz.comment.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.comment.dto.CreateCommentRequest;
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

    /**
     * 发表评论
     *
     * @param request 创建评论请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 新创建的评论 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateCommentRequest request,
                               @RequestHeader("X-User-Id") Long userId) {
        Long commentId = commentService.createComment(userId, request);
        return Result.success("评论成功", commentId);
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
                               @RequestHeader("X-User-Id") Long userId) {
        commentService.deleteComment(userId, commentId);
        return Result.success("删除成功");
    }

    /**
     * 分页查询帖子的评论列表
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始，默认 1）
     * @param size          每页条数（默认 20）
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 评论列表（含嵌套子回复）
     */
    @GetMapping("/list")
    public Result<PageResult<CommentVO>> list(@RequestParam("postId") Long postId,
                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size,
                                        @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return Result.success(commentService.listComments(postId, page, size, currentUserId));
    }

    /**
     * 点赞 / 取消点赞评论
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户 ID（由 Gateway 注入）
     * @return 操作后的点赞数
     */
    @PostMapping("/{commentId}/like")
    public Result<Integer> toggleLike(@PathVariable("commentId") Long commentId,
                                       @RequestHeader("X-User-Id") Long userId) {
        int likes = commentService.toggleLike(userId, commentId);
        return Result.success(likes);
    }
}
