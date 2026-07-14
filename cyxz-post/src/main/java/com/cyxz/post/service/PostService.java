package com.cyxz.post.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.vo.PostVO;
import jakarta.servlet.http.HttpServletRequest;

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
     * <p>将帖子状态设为 2（已删除），不做物理删除。
     * 校验帖子归属权，非作者本人无权删除。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void deletePost(Long userId, Long postId);

    /**
     * 根据 ID 查询帖子详情
     * <p>已删除的帖子不可查看，草稿仅作者本人可查看。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（可为 null，游客访问）
     * @return 帖子视图对象（含作者信息、分类名称）
     */
    PostVO getById(Long postId, Long currentUserId);

    /**
     * 分页查询帖子列表（仅已发布）
     * <p>按创建时间倒序排列，可按分类筛选。
     *
     * @param categoryId    分类 ID（可为 null，null 时查全部分类）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null，用于查询点赞状态）
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listPosts(Long categoryId, int page, int size, Long currentUserId);

    /**
     * 查询当前用户的帖子列表
     * <p>包含草稿、已发布和已删除，按创建时间倒序。
     *
     * @param userId 当前用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果（含总条数）
     */
    PageResult<PostVO> listByUserId(Long userId, int page, int size);

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
     * 切换帖子点赞状态
     * <p>已点赞则取消，未点赞则添加。使用 Redis Set 存储用户点赞关系，
     * 同时更新数据库中的点赞数。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     * @return 更新后的点赞数
     */
    Integer toggleLike(Long userId, Long postId);

    /**
     * 切换帖子收藏状态
     * <p>已收藏则取消，未收藏则添加。使用 Redis Set 存储用户收藏关系，
     * 同时更新数据库中的收藏数。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     * @return 更新后的收藏数
     */
    Integer toggleCollect(Long userId, Long postId);

    /**
     * 记录浏览
     * <p>用户进入帖子详情页时调用，Redis 去重（30min 内同一用户/IP 只算一次），
     * 去重通过则 Hash 增量 +1，由定时任务刷库到 post.views。
     *
     * @param postId  帖子 ID
     * @param userId  当前登录用户 ID（可为 null，游客按 IP 去重）
     * @param request HTTP 请求（用于获取 IP）
     */
    void recordView(Long postId, Long userId, HttpServletRequest request);
}
