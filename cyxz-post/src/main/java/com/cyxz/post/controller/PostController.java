package com.cyxz.post.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.service.PostInteractionService;
import com.cyxz.post.service.PostService;
import com.cyxz.post.service.SensitiveWordService;
import com.cyxz.post.vo.TodayStatsVO;
import com.cyxz.post.vo.PostInfoVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.PostVO;
import com.cyxz.post.vo.ReceivedLikeVO;
import com.cyxz.post.vo.DashboardVO;
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
    private final PostInteractionService postInteractionService;
    private final SensitiveWordService sensitiveWordService;

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
     * @return 帖子详情（含作者信息、板块名称）
     */
    @GetMapping("/{postId}")
    public Result<PostVO> getById(@PathVariable("postId") Long postId,
                                  @CurrentUser(required = false) Long currentUserId) {
        return Result.success(postService.getById(postId, currentUserId));
    }

    /**
     * 分页查询帖子列表（仅已发布）
     *
     * @param sectionId     板块 ID（可选）
     * @param circleId      圈子 ID（可选）
     * @param sortBy        排序方式：latest/hot，默认 latest
     * @param page          页码（从 1 开始，默认 1）
     * @param size          每页条数（默认 10）
     * @param currentUserId 当前登录用户 ID（由 Gateway 注入，游客为 null）
     * @return 帖子列表
     */
    @GetMapping("/list")
    public Result<PageResult<PostVO>> list(@RequestParam(value = "sectionId", required = false) Long sectionId,
                                     @RequestParam(value = "circleId", required = false) Long circleId,
                                     @RequestParam(value = "sortBy", defaultValue = "latest") String sortBy,
                                     @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                     @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
                                     @CurrentUser(required = false) Long currentUserId) {
        return Result.success(postService.listPosts(sectionId, circleId, sortBy, page, size, currentUserId));
    }

    /**
     * 分页查询关注动态（关注用户发布的帖子）
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param page   页码（从 1 开始，默认 1）
     * @param size   每页条数（默认 10）
     * @return 关注用户的帖子列表
     */
    @GetMapping("/following")
    public Result<PageResult<PostVO>> listFollowing(@CurrentUser Long userId,
                                                    @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                    @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(postService.listFollowingPosts(userId, page, size));
    }

    /**
     * 查询当前用户的帖子列表（含草稿和已删除）
     *
     * @param userId    当前登录用户 ID（由 Gateway 注入）
     * @param page      页码（从 1 开始，默认 1）
     * @param size      每页条数（默认 10）
     * @param sortField 排序字段（createTime/views/likes/collections），默认 createTime
     * @param sortOrder 排序方向（asc/desc），默认 desc
     * @return 帖子列表
     */
    @GetMapping("/user")
    public Result<PageResult<PostVO>> listByUser(@CurrentUser Long userId,
                                           @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                           @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
                                           @RequestParam(value = "sortField", defaultValue = "create_time") String sortField,
                                           @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {
        return Result.success(postService.listByUserId(userId, page, size, sortField, sortOrder));
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
                                                 @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                 @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
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
                                                  @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                  @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
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
        postInteractionService.likePost(userId, postId);
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
        postInteractionService.unlikePost(userId, postId);
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
        postInteractionService.collectPost(userId, postId);
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
        postInteractionService.uncollectPost(userId, postId);
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
        postInteractionService.recordView(postId, currentUserId, request);
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
     * 获取数据中心仪表盘数据
     * <p>包含概览统计、月度趋势、板块分布和 Top 作品排行。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(@CurrentUser Long userId) {
        return Result.success(postService.getDashboard(userId));
    }

    /**
     * 获取今日新增互动统计
     * <p>用于创作首页工作台展示今日新增的点赞、收藏、评论数。
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 今日统计
     */
    @GetMapping("/today")
    public Result<TodayStatsVO> getTodayStats(@CurrentUser Long userId) {
        return Result.success(postService.getTodayStats(userId));
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
                                            @RequestParam(value = "limit", defaultValue = PageConstants.SIZE_5_STR) int limit) {
        return Result.success(postService.getTopPosts(userId, limit));
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
     * 批量统计各圈子的已发布帖子数（内部接口，供 circle 服务定时刷新）
     *
     * @param circleIds 圈子 ID 集合
     * @return 圈子 ID 到帖子数的映射
     */
    @GetMapping("/internal/batch-circle-post-count")
    public Result<Map<Long, Integer>> batchCountByCircle(@RequestParam("circleIds") Set<Long> circleIds) {
        return Result.success(postService.batchCountByCircle(circleIds));
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
                                                                @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                                @RequestParam(value = "size", defaultValue = PageConstants.SIZE_20_STR) int size) {
        return Result.success(postService.getReceivedLikes(userId, page, size));
    }

    /**
     * 置顶帖子
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param postId 帖子 ID
     * @return 操作结果
     */
    @PutMapping("/{postId}/pin")
    public Result<Void> pinPost(@CurrentUser Long userId, @PathVariable("postId") Long postId) {
        postService.pinPost(userId, postId);
        return Result.success("置顶成功");
    }

    /**
     * 取消置顶帖子
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param postId 帖子 ID
     * @return 操作结果
     */
    @DeleteMapping("/{postId}/pin")
    public Result<Void> unpinPost(@CurrentUser Long userId, @PathVariable("postId") Long postId) {
        postService.unpinPost(userId, postId);
        return Result.success("取消置顶成功");
    }

    /**
     * 批量操作帖子
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @param body   批量操作请求体，包含 postIds（帖子 ID 列表）和 action（操作类型）
     * @return 操作结果
     */
    @PostMapping("/batch")
    public Result<Void> batchOperate(@CurrentUser Long userId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> postIds = ((List<String>) body.get("postIds")).stream().map(Long::valueOf).toList();
        String action = (String) body.get("action");
        postService.batchOperate(userId, postIds, action);
        return Result.success("批量操作成功");
    }

    // ===== 审核接口（平台管理员） =====

    /**
     * 待审核帖子列表
     *
     * @param page 页码（从 1 开始，默认 1）
     * @param size 每页条数（默认 10）
     * @return 待审核帖子分页列表
     */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @GetMapping("/admin/review/pending")
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
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @PutMapping("/admin/review/{postId}/approve")
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
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @PutMapping("/admin/review/{postId}/reject")
    public Result<Void> rejectPost(@PathVariable Long postId, @RequestBody Map<String, String> body) {
        postService.rejectPost(postId, body.get("reason"));
        return Result.success("已拒绝");
    }

    // ===== 帖子管理接口（平台管理员） =====

    /**
     * 管理员帖子列表（全量，含各种状态）
     *
     * @param status  帖子状态筛选（null=全部：0=草稿 1=待审核 2=已通过 3=拒绝 4=已删除）
     * @param keyword 标题关键词
     * @param page    页码
     * @param size    每页条数
     */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @GetMapping("/admin/list")
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
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/admin/{postId}")
    public Result<Void> adminDeletePost(@PathVariable Long postId) {
        postService.adminDeletePost(postId);
        return Result.success("删除成功");
    }

    /**
     * 敏感词手动检测
     * <p>用户发布前自行检测标题和正文，返回命中的敏感词列表。为空表示通过。
     *
     * @param body 检测请求体，包含 title（标题）和 content（正文）
     * @return 命中的敏感词集合，为空表示无敏感词
     */
    @PostMapping("/check-sensitive")
    public Result<Set<String>> checkSensitive(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");
        return Result.success(sensitiveWordService.check(title, content));
    }
}
