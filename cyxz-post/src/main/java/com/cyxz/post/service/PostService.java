package com.cyxz.post.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.api.vo.PostInfoVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.PostVO;
import com.cyxz.post.vo.ReceivedLikeVO;
import com.cyxz.post.vo.DashboardVO;
import com.cyxz.post.vo.TodayStatsVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子服务接口
 */
public interface PostService {

    /**
     * 创建帖子
     * <p>将前端传入的标题、正文、图片、标签等信息持久化到 post 表。
     * 图片和标签以逗号分隔的字符串形式存储。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建帖子请求
     * @return 新创建的帖子 ID
     */
    Long createPost(Long userId, CreatePostRequest request);

    /**
     * 更新帖子
     * <p>仅更新非 null 字段，不做全量覆盖。
     * 校验帖子归属权，非作者本人无权修改。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新帖子请求（须包含帖子 ID）
     */
    void updatePost(Long userId, UpdatePostRequest request);

    /**
     * 删除帖子（逻辑删除）
     * <p>将帖子状态设为 4（已删除），不做物理删除。
     * 校验帖子归属权，非作者本人无权删除。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void deletePost(Long userId, Long postId);

    /**
     * 彻底删除帖子（物理删除 + 级联清理关联数据）
     * <p>仅允许删除 status=2 的帖子（已在回收站中的帖子），
     * 同时清理评论、评论点赞、帖子点赞、帖子收藏等关联数据。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void hardDeletePost(Long userId, Long postId);

    /**
     * 根据 ID 查询帖子详情
     * <p>已删除的帖子不可查看，草稿仅作者本人可查看。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（可为 null，游客访问）
     * @return 帖子视图对象（含作者信息、板块名称）
     */
    PostVO getById(Long postId, Long currentUserId);

    /**
     * 分页查询帖子列表（仅已发布）
     * <p>支持按板块、圈子筛选和排序。
     *
     * @param sectionId     板块 ID（可为 null）
     * @param circleId      圈子 ID（可为 null）
     * @param sortBy        排序方式：latest 按创建时间倒序，hot 按热度（点赞→评论→收藏→浏览）倒序
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null，用于查询点赞状态）
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listPosts(Long sectionId, Long circleId, String sortBy, int page, int size, Long currentUserId);

    /**
     * 查询当前用户的帖子列表
     * <p>包含草稿、已发布和已删除，支持排序。
     *
     * @param userId    当前用户 ID
     * @param page      页码（从 1 开始）
     * @param size      每页条数
     * @param sortField 排序字段（createTime/views/likes/collections），默认 createTime
     * @param sortOrder 排序方向（asc/desc），默认 desc
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listByUserId(Long userId, int page, int size, String sortField, String sortOrder);

    /**
     * 查询指定用户的已发布帖子列表
     * <p>个人空间使用，仅返回 status=1 的帖子，按创建时间倒序。
     *
     * @param targetUserId    目标用户 ID
     * @param currentUserId   当前登录用户 ID（可为 null，用于查询点赞/收藏状态）
     * @param page            页码（从 1 开始）
     * @param size            每页条数
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listByTargetUserId(Long targetUserId, Long currentUserId, int page, int size);

    /**
     * 查询指定用户的收藏帖子列表
     * <p>从 Redis 中获取用户收藏的帖子 ID，批量查询帖子详情。
     *
     * @param targetUserId    目标用户 ID
     * @param currentUserId   当前登录用户 ID（可为 null）
     * @param page            页码（从 1 开始）
     * @param size            每页条数
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listFavorites(Long targetUserId, Long currentUserId, int page, int size);

    /**
     * 获取用户帖子统计数据
     * <p>用于数据中心，SQL 聚合统计当前用户所有已发布帖子的总浏览、总点赞、总收藏。
     *
     * @param userId 当前用户 ID
     * @return 统计数据 VO
     */
    PostStatsVO getPostStats(Long userId);

    /**
     * 查询用户作品排行榜（按浏览量倒序）
     *
     * @param userId 当前用户 ID
     * @param limit  返回条数
     * @return 帖子列表
     */
    List<PostVO> getTopPosts(Long userId, int limit);

    /**
     * 查询帖子信息（内部接口）
     *
     * @param postId 帖子 ID
     * @return 帖子信息（标题、作者 ID 等）
     */
    PostInfoVO getPostInfo(Long postId);

    /**
     * 批量查询帖子简要信息（内部接口）
     * <p>用于评论服务一次性查询多个帖子标题，避免 N+1 Feign 调用。
     *
     * @param postIds 帖子 ID 集合
     * @return 帖子信息列表
     */
    List<PostInfoVO> batchGetPostInfo(Set<Long> postIds);

    /**
     * 查询用户收到的点赞列表
     *
     * @param userId 当前用户 ID
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<ReceivedLikeVO> getReceivedLikes(Long userId, int page, int size);

    /**
     * 获取数据中心仪表盘数据
     * <p>包含概览统计、月度趋势、板块分布和 Top 5 作品排行。
     *
     * @param userId 当前用户 ID
     * @return 仪表盘 VO
     */
    DashboardVO getDashboard(Long userId);

    /**
     * 获取今日新增互动统计
     * <p>用于创作首页工作台展示今日新增的点赞、收藏、评论数。
     *
     * @param userId 当前用户 ID
     * @return 今日统计 VO
     */
    TodayStatsVO getTodayStats(Long userId);

    /**
     * 置顶帖子
     * <p>仅已发布帖子可置顶，每人最多置顶 3 条。
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     */
    void pinPost(Long userId, Long postId);

    /**
     * 取消置顶帖子
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     */
    void unpinPost(Long userId, Long postId);

    /**
     * 分页查询关注动态（已加入圈子的帖子）
     * <p>按创建时间倒序排列，仅返回已发布帖子。
     *
     * @param userId 当前用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listFollowingPosts(Long userId, int page, int size);

    /**
     * 批量操作帖子
     * <p>支持批量发布和批量删除。
     *
     * @param userId  当前用户 ID
     * @param postIds 帖子 ID 列表
     * @param action  操作类型（publish / delete）
     */
    void batchOperate(Long userId, List<Long> postIds, String action);

    /**
     * 审核通过帖子
     * @param postId 帖子 ID
     */
    void approvePost(Long postId);

    /**
     * 审核拒绝帖子
     */
    void rejectPost(Long postId, String reason);

    /**
     * 圈子维度审核通过帖子（圈主/圈子管理员使用）
     * <p>校验帖子归属当前圈子后执行审核。
     *
     * @param circleId 圈子 ID
     * @param postId   帖子 ID
     */
    void approvePostByCircle(Long circleId, Long postId);

    /**
     * 圈子维度审核拒绝帖子（圈主/圈子管理员使用）
     * <p>校验帖子归属当前圈子后执行审核。
     *
     * @param circleId 圈子 ID
     * @param postId   帖子 ID
     * @param reason   拒绝原因
     */
    void rejectPostByCircle(Long circleId, Long postId, String reason);

    /**
     * 查询待审核帖子列表
     */
    PageResult<PostVO> listPendingReview(int page, int size);

    /**
     * 圈子维度的待审核帖子列表（圈子管理员/圈主使用）
     */
    PageResult<PostVO> listPendingReviewByCircle(Long circleId, int page, int size);

    /**
     * 管理员帖子列表（全量，含各种状态）
     *
     * @param status  帖子状态筛选（null=全部）
     * @param keyword 标题关键词（null=不筛选）
     * @param page    页码
     * @param size    每页条数
     */
    PageResult<PostVO> listAllForAdmin(Integer status, String keyword, int page, int size);

    /**
     * 管理员删除帖子（逻辑删除，不校验作者归属）
     *
     * @param postId 帖子 ID
     */
    void adminDeletePost(Long postId);

    /**
     * 圈子维度删帖（圈主/圈子管理员删除本圈帖子）
     *
     * @param circleId 圈子 ID
     * @param postId   帖子 ID
     */
    void deletePostByCircle(Long circleId, Long postId);

    /**
     * 批量统计各圈子的已发布帖子数（内部接口）
     * <p>供 circle 服务定时刷新帖子数。
     * @param circleIds 圈子 ID 集合
     * @return 圈子 ID 到帖子数的映射（无帖子的圈子返回 0）
     */
    Map<Long, Integer> batchCountByCircle(Set<Long> circleIds);
}
