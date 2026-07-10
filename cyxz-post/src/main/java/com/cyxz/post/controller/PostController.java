package com.cyxz.post.controller;

import com.cyxz.common.base.Result;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子控制器
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 创建帖子
     *
     * @param request 创建帖子请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 新创建的帖子 ID
     */
    @PostMapping
    public Result<Long> create(@RequestBody CreatePostRequest request,
                               @RequestHeader("X-User-Id") Long userId) {
        Long postId = postService.createPost(userId, request);
        return Result.success("创建成功", postId);
    }

    /**
     * 更新帖子
     *
     * @param request 更新帖子请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody UpdatePostRequest request,
                               @RequestHeader("X-User-Id") Long userId) {
        postService.updatePost(userId, request);
        return Result.success("更新成功");
    }

    /**
     * 删除帖子（逻辑删除）
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{postId}")
    public Result<Void> delete(@PathVariable("postId") Long postId,
                               @RequestHeader("X-User-Id") Long userId) {
        postService.deletePost(userId, postId);
        return Result.success("删除成功");
    }

    /**
     * 查询帖子详情
     *
     * @param postId 帖子 ID
     * @return 帖子详情（含作者信息、分类名称）
     */
    @GetMapping("/{postId}")
    public Result<PostVO> getById(@PathVariable("postId") Long postId) {
        return Result.success(postService.getById(postId));
    }

    /**
     * 分页查询帖子列表（仅已发布）
     *
     * @param categoryId 分类 ID（可选，null 时查全部分类）
     * @param page       页码（从 1 开始，默认 1）
     * @param size       每页条数（默认 10）
     * @return 帖子列表
     */
    @GetMapping("/list")
    public Result<List<PostVO>> list(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size) {
        return Result.success(postService.listPosts(categoryId, page, size));
    }

    /**
     * 查询用户的帖子列表
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始，默认 1）
     * @param size   每页条数（默认 10）
     * @return 帖子列表（含草稿和已发布，不含已删除）
     */
    @GetMapping("/user/{userId}")
    public Result<List<PostVO>> listByUser(@PathVariable("userId") Long userId,
                                           @RequestParam(value = "page", defaultValue = "1") int page,
                                           @RequestParam(value = "size", defaultValue = "10") int size) {
        return Result.success(postService.listByUserId(userId, page, size));
    }
}
