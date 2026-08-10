package com.cyxz.post.controller;

import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.post.dto.RejectPostRequest;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子管理控制器（平台管理员）
 * <p>承载平台运营侧的帖子审核与内容管理接口，与 C 端 {@link PostController} 隔离。
 * <p>统一挂载在 {@code /admin/post} 前缀下，由网关 {@code /api/admin/**} 路由统一入口。
 */
@Tag(name = "帖子管理（平台管理员）", description = "帖子审核与内容管理")
@RestController
@RequestMapping("/admin/post")
@RequiredArgsConstructor
public class PostAdminController {

    private final PostService postService;

    /**
     * 待审核帖子列表
     *
     * @param page 页码（从 1 开始，默认 1）
     * @param size 每页条数（默认 10）
     * @return 待审核帖子分页列表
     */
    @Operation(summary = "待审核帖子列表")
    @PreAuthorize("hasAuthority('post:review:list')")
    @GetMapping("/review/pending")
    public Result<PageResult<PostVO>> listPendingReview(@RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                         @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(postService.listPendingReview(page, size));
    }

    /**
     * 审核通过
     *
     * @param postId 帖子 ID
     * @return 操作结果
     */
    @Operation(summary = "审核通过")
    @PreAuthorize("hasAuthority('post:review:approve')")
    @PreventRepeat(interval = 3)
    @PutMapping("/review/{postId}/approve")
    public Result<Void> approvePost(@PathVariable Long postId) {
        postService.approvePost(postId);
        return Result.success("审核通过");
    }

    /**
     * 审核拒绝
     *
     * @param postId 帖子 ID
     * @param body   拒绝请求体，包含 reason（拒绝原因）
     * @return 操作结果
     */
    @Operation(summary = "审核拒绝")
    @PreAuthorize("hasAuthority('post:review:reject')")
    @PutMapping("/review/{postId}/reject")
    public Result<Void> rejectPost(@PathVariable Long postId, @Valid @RequestBody RejectPostRequest request) {
        postService.rejectPost(postId, request.getReason());
        return Result.success("已拒绝");
    }

    /**
     * 管理员帖子列表（全量，含各种状态）
     *
     * @param status  帖子状态筛选（null=全部：0=草稿 1=待审核 2=已通过 3=拒绝 4=已删除）
     * @param keyword 标题关键词
     * @param page    页码
     * @param size    每页条数
     */
    @Operation(summary = "管理员帖子列表（全量，含各种状态）")
    @PreAuthorize("hasAuthority('post:admin:list')")
    @GetMapping("/list")
    public Result<PageResult<PostVO>> listAllForAdmin(@RequestParam(value = "status", required = false) Integer status,
                                                       @RequestParam(value = "keyword", required = false) String keyword,
                                                       @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                       @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(postService.listAllForAdmin(status, keyword, page, size));
    }

    /**
     * 管理员删除帖子（逻辑删除，不校验作者归属）
     *
     * @param postId 帖子 ID
     */
    @Operation(summary = "管理员删除帖子（逻辑删除，不校验作者归属）")
    @PreAuthorize("hasAuthority('post:admin:delete')")
    @DeleteMapping("/{postId}")
    public Result<Void> adminDeletePost(@PathVariable Long postId) {
        postService.adminDeletePost(postId);
        return Result.success("删除成功");
    }
}
