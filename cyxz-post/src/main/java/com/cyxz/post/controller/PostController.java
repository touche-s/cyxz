package com.cyxz.post.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostInfoVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.PostVO;
import com.cyxz.post.vo.ReceivedLikeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子控制器
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 创建帖子（发布）
     *
     * @param request 创建帖子请求
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 新创建的帖子 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreatePostRequest request,
                               @CurrentUser Long userId) {
        request.setStatus(1);
        Long postId = postService.createPost(userId, request);
        return Result.success("发布成功", postId);
    }

    /**
     * 新建草稿
     *
     * @param request 草稿请求（仅需任一项有内容）
     * @param userId  当前登录用户 ID（由 Gateway 注入）
     * @return 帖子 ID
     */
    @PostMapping("/draft")
    public Result<Long> saveDraft(@Valid @RequestBody CreatePostRequest request,
                                  @CurrentUser Long userId) {
        request.setStatus(0);
        Long postId = postService.createPost(userId, request);
        return Result.success("草稿保存成功", postId);
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
                               @CurrentUser Long userId) {
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
                               @CurrentUser Long userId) {
        postService.deletePost(userId, postId);
        return Result.success("删除成功");
    }

    /**
     * 彻底删除帖子（物理删除 + 级联清理）
     * <p>仅允许删除回收站中（status=2）的帖子，
     * 同时清理评论、评论点赞、帖子点赞、帖子收藏。
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{postId}/permanent")
    public Result<Void> deletePermanent(@PathVariable("postId") Long postId,
                                        @CurrentUser Long userId) {
        postService.hardDeletePost(userId, postId);
        return Result.success("彻底删除成功");
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
                                  @CurrentUser(required = false) Long currentUserId) {
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
                                     @CurrentUser(required = false) Long currentUserId) {
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
    public Result<PageResult<PostVO>> listByUser(@CurrentUser Long userId,
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
                                                 @CurrentUser(required = false) Long currentUserId) {
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
                                                  @CurrentUser(required = false) Long currentUserId) {
        return Result.success(postService.listFavorites(targetUserId, currentUserId, page, size));
    }

    /**
     * 点赞帖子（幂等）
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PutMapping("/{postId}/like")
    public Result<Void> like(@PathVariable("postId") Long postId,
                             @CurrentUser Long userId) {
        postService.likePost(userId, postId);
        return Result.success();
    }

    /**
     * 取消点赞帖子（幂等）
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{postId}/like")
    public Result<Void> unlike(@PathVariable("postId") Long postId,
                               @CurrentUser Long userId) {
        postService.unlikePost(userId, postId);
        return Result.success();
    }

    /**
     * 收藏帖子（幂等）
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @PutMapping("/{postId}/collect")
    public Result<Void> collect(@PathVariable("postId") Long postId,
                                @CurrentUser Long userId) {
        postService.collectPost(userId, postId);
        return Result.success();
    }

    /**
     * 取消收藏帖子（幂等）
     *
     * @param postId 帖子 ID
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/{postId}/collect")
    public Result<Void> uncollect(@PathVariable("postId") Long postId,
                                  @CurrentUser Long userId) {
        postService.uncollectPost(userId, postId);
        return Result.success();
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
                                   @CurrentUser(required = false) Long currentUserId,
                                   HttpServletRequest request) {
        postService.recordView(postId, currentUserId, request);
        return Result.success();
    }

    /**
     * 获取当前用户的帖子统计数据
     * <p>用于数据中心，统计已发布帖子的总浏览、总点赞、总收藏。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<PostStatsVO> getStats(@CurrentUser Long userId) {
        return Result.success(postService.getPostStats(userId));
    }

    /**
     * 获取指定用户的帖子统计数据
     * <p>用于查看他人个人空间时展示获赞、浏览等数据。
     *
     * @param targetUserId 目标用户 ID
     * @return 统计数据
     */
    @GetMapping("/user/{targetUserId}/stats")
    public Result<PostStatsVO> getStatsByUserId(@PathVariable("targetUserId") Long targetUserId) {
        return Result.success(postService.getPostStats(targetUserId));
    }

    /**
     * 查询用户作品排行榜（按浏览量倒序）
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param limit  返回条数（默认 5）
     * @return 帖子列表
     */
    @GetMapping("/top")
    public Result<List<PostVO>> getTopPosts(@CurrentUser Long userId,
                                            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return Result.success(postService.getTopPosts(userId, limit));
    }

    /**
     * 查询帖子作者 ID（内部接口，供 comment 服务通过 Feign 调用）
     *
     * @param postId 帖子 ID
     * @return 帖子作者 ID
     */
    @GetMapping("/internal/{postId}/author")
    public Result<Long> getPostAuthor(@PathVariable("postId") Long postId) {
        return Result.success(postService.getPostAuthor(postId));
    }

    /**
     * 查询帖子信息（内部接口，供 comment 服务通过 Feign 调用）
     *
     * @param postId 帖子 ID
     * @return 帖子信息（标题、作者 ID 等）
     */
    @GetMapping("/internal/{postId}/info")
    public Result<Map<String, Object>> getPostInfo(@PathVariable("postId") Long postId) {
        return Result.success(postService.getPostInfo(postId));
    }

    /**
     * 批量查询帖子简要信息（内部接口，供 comment 服务通过 Feign 调用）
     *
     * @param postIds 帖子 ID 集合
     * @return 帖子信息列表
     */
    @GetMapping("/internal/batch-info")
    public Result<List<PostInfoVO>> batchGetPostInfo(@RequestParam("postIds") Set<Long> postIds) {
        return Result.success(postService.batchGetPostInfo(postIds));
    }

    /**
     * 查询用户收到的点赞列表
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码
     * @param size   每页条数
     * @return 点赞列表
     */
    @GetMapping("/received-likes")
    public Result<PageResult<ReceivedLikeVO>> getReceivedLikes(@CurrentUser Long userId,
                                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                                @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(postService.getReceivedLikes(userId, page, size));
    }
}
