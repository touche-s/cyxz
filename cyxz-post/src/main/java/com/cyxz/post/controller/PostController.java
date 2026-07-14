package com.cyxz.post.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<Long> create(@Valid @RequestBody CreatePostRequest request,
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
    public Result<Void> update(@Valid @RequestBody UpdatePostRequest request,
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
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 帖子详情（含作者信息、分类名称）
     */
    @GetMapping("/{postId}")
    public Result<PostVO> getById(@PathVariable("postId") Long postId,
                                  @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return Result.success(postService.getById(postId, currentUserId));
    }

    /**
     * 分页查询帖子列表（仅已发布）
     *
     * @param categoryId    分类 ID（可选，null 时查全部分类）
     * @param page          页码（从 1 开始，默认 1）
     * @param size          每页条数（默认 10）
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 帖子列表
     */
    @GetMapping("/list")
    public Result<PageResult<PostVO>> list(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return Result.success(postService.listPosts(categoryId, page, size, currentUserId));
    }

    /**
     * 查询当前用户的帖子列表（含草稿和已删除）
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码（从 1 开始，默认 1）
     * @param size   每页条数（默认 10）
     * @return 帖子列表
     */
    @GetMapping("/user")
    public Result<PageResult<PostVO>> listByUser(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(value = "page", defaultValue = "1") int page,
                                           @RequestParam(value = "size", defaultValue = "10") int size) {
        return Result.success(postService.listByUserId(userId, page, size));
    }

    /**
     * 查询指定用户的已发布帖子列表（个人空间 - 作品 tab）
     *
     * @param targetUserId    目标用户 ID
     * @param page            页码（从 1 开始，默认 1）
     * @param size            每页条数（默认 10）
     * @param currentUserId   当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 帖子列表
     */
    @GetMapping("/user/{targetUserId}/posts")
    public Result<PageResult<PostVO>> listByTargetUser(@PathVariable("targetUserId") Long targetUserId,
                                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return Result.success(postService.listByTargetUserId(targetUserId, currentUserId, page, size));
    }

    /**
     * 查询指定用户的收藏帖子列表（个人空间 - 收藏 tab）
     *
     * @param targetUserId    目标用户 ID
     * @param page            页码（从 1 开始，默认 1）
     * @param size            每页条数（默认 10）
     * @param currentUserId   当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 帖子列表
     */
    @GetMapping("/user/{targetUserId}/favorites")
    public Result<PageResult<PostVO>> listUserFavorites(@PathVariable("targetUserId") Long targetUserId,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                                  @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return Result.success(postService.listFavorites(targetUserId, currentUserId, page, size));
    }

    /**
     * 点赞 / 取消点赞帖子
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 更新后的点赞数
     */
    @PostMapping("/{postId}/like")
    public Result<Integer> like(@PathVariable("postId") Long postId,
                                @RequestHeader("X-User-Id") Long userId) {
        Integer likes = postService.toggleLike(userId, postId);
        return Result.success(likes);
    }

    /**
     * 收藏 / 取消收藏帖子
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 更新后的收藏数
     */
    @PostMapping("/{postId}/collect")
    public Result<Integer> collect(@PathVariable("postId") Long postId,
                                   @RequestHeader("X-User-Id") Long userId) {
        Integer collections = postService.toggleCollect(userId, postId);
        return Result.success(collections);
    }

    /**
     * 记录浏览
     * <p>前端进入帖子详情页时静默调用一次，Redis 去重 + 增量计数，定时刷库。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @param request       HTTP 请求（用于获取游客 IP）
     * @return 操作结果
     */
    @PostMapping("/{postId}/view")
    public Result<Void> recordView(@PathVariable("postId") Long postId,
                                   @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                   HttpServletRequest request) {
        postService.recordView(postId, currentUserId, request);
        return Result.success();
    }
}
