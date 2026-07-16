package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.utils.IpUtil;
import com.cyxz.post.vo.PostInfoVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.ReceivedLikeVO;
import com.cyxz.user.service.UserRemoteService;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.entity.CategoryPO;
import com.cyxz.post.entity.PostCollectPO;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.CategoryService;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostVO;
import com.cyxz.user.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final CategoryService categoryService;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final UserRemoteService userRemoteService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建帖子
     * <p>将前端传入的标题、正文、图片、标签等信息持久化到 post 表。
     * 图片和标签列表以逗号分隔的字符串形式存储。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建帖子请求
     * @return 新创建的帖子 ID
     */
    @Override
    public Long createPost(Long userId, CreatePostRequest request) {
        PostPO po = new PostPO();
        po.setUserId(userId);
        po.setCategoryId(request.getCategoryId());
        po.setTitle(request.getTitle());
        po.setContent(request.getContent());
        po.setCover(request.getCover());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            po.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            po.setTags(String.join(",", request.getTags()));
        }
        po.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        po.setLikes(0);
        po.setComments(0);
        po.setViews(0);
        po.setCollections(0);
        postMapper.insert(po);
        log.info("创建帖子成功: postId={}, userId={}", po.getId(), userId);
        return po.getId();
    }

    /**
     * 更新帖子
     * <p>仅更新非 null 字段，不做全量覆盖。
     * 校验帖子归属权，非作者本人无权修改。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新帖子请求（须包含帖子 ID）
     */
    @Override
    public void updatePost(Long userId, UpdatePostRequest request) {
        PostPO po = postMapper.selectById(request.getIdAsLong());
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getCategoryId() != null) {
            po.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getTitle())) {
            po.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            po.setContent(request.getContent());
        }
        if (request.getCover() != null) {
            po.setCover(request.getCover());
        }
        if (request.getImages() != null) {
            po.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null) {
            po.setTags(String.join(",", request.getTags()));
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
        postMapper.updateById(po);
        log.info("更新帖子成功: postId={}, userId={}", po.getId(), userId);
    }

    /**
     * 删除帖子（逻辑删除）
     * <p>仅将帖子状态改为 2（已删除），不物理删除数据，可在回收站恢复。
     * 校验帖子归属权，非作者本人无权删除。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    @Override
    public void deletePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        po.setStatus(2);
        postMapper.updateById(po);
        log.info("软删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 根据 ID 查询帖子详情
     * <p>已发布帖子所有人可查看，草稿和已删除帖子仅作者本人可查看。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子视图对象（含作者信息、分类名称）
     */
    @Override
    public PostVO getById(Long postId, Long currentUserId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        // 已删除：仅作者本人可查看
        if (po.getStatus() == 2 && !po.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        // 草稿：仅作者本人可查看
        if (po.getStatus() == 0 && !po.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        Map<Long, UserProfileVO> userMap = userRemoteService.batchGetByIds(Set.of(po.getUserId()));
        Map<Long, CategoryPO> categoryMap = categoryService.getByIds(extractCategoryIds(List.of(po)));
        Set<Long> likedPostIds = getLikedPostIds(currentUserId);
        Set<Long> collectedPostIds = getCollectedPostIds(currentUserId);
        return convertToVO(po, userMap, categoryMap, likedPostIds, collectedPostIds);
    }

    /**
     * 分页查询帖子列表（仅已发布）
     * <p>按创建时间倒序排列，可按分类筛选。
     *
     * @param categoryId    分类 ID（可为 null，null 时查全部分类）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listPosts(Long categoryId, int page, int size, Long currentUserId) {
        Page<PostPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(PostPO::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        return PageResult.of(fillPostVOList(result.getRecords(), currentUserId), result.getTotal(), page, size);
    }

    /**
     * 查询用户的帖子列表
     * <p>包含草稿、已发布和已删除，按创建时间倒序。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listByUserId(Long userId, int page, int size) {
        Page<PostPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, userId);
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        Map<Long, UserProfileVO> userMap = userRemoteService.batchGetByIds(result.getRecords().stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        Map<Long, CategoryPO> categoryMap = categoryService.getByIds(extractCategoryIds(result.getRecords()));
        Set<Long> likedPostIds = getLikedPostIds(userId);
        Set<Long> collectedPostIds = getCollectedPostIds(userId);

        List<PostVO> vos = result.getRecords().stream()
                .map(po -> convertToVO(po, userMap, categoryMap, likedPostIds, collectedPostIds))
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 查询指定用户的已发布帖子列表
     * <p>个人空间使用，仅返回 status=1 的帖子，按创建时间倒序。
     *
     * @param targetUserId    目标用户 ID
     * @param currentUserId   当前登录用户 ID（可为 null）
     * @param page            页码（从 1 开始）
     * @param size            每页条数
     * @return 分页结果（含总条数）
     */
    @Override
    public PageResult<PostVO> listByTargetUserId(Long targetUserId, Long currentUserId, int page, int size) {
        Page<PostPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, targetUserId);
        wrapper.eq(PostPO::getStatus, 1);
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        return PageResult.of(fillPostVOList(result.getRecords(), currentUserId), result.getTotal(), page, size);
    }

    /**
     * 查询指定用户的收藏帖子列表
     * <p>从 post_collect 表查询用户收藏的帖子 ID，批量查询帖子详情。
     *
     * @param targetUserId    目标用户 ID
     * @param currentUserId   当前登录用户 ID（可为 null）
     * @param page            页码（从 1 开始）
     * @param size            每页条数
     * @return 分页结果（含总条数）
     */
    @Override
    public PageResult<PostVO> listFavorites(Long targetUserId, Long currentUserId, int page, int size) {
        // 从 post_collect 表获取目标用户收藏的帖子 ID（仅 status=1）
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, targetUserId)
                .eq(PostCollectPO::getStatus, 1)
                .orderByDesc(PostCollectPO::getCreateTime);
        Page<PostCollectPO> pageParam = new Page<>(page, size);
        Page<PostCollectPO> collectPage = postCollectMapper.selectPage(pageParam, wrapper);

        List<PostCollectPO> records = collectPage.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), collectPage.getTotal(), page, size);
        }

        List<Long> postIds = records.stream()
                .map(PostCollectPO::getPostId)
                .collect(Collectors.toList());

        // 批量查询帖子，过滤掉已删除和未发布的
        List<PostPO> posts = postMapper.selectBatchIds(postIds).stream()
                .filter(po -> po.getStatus() == 1)
                .collect(Collectors.toList());

        return PageResult.of(fillPostVOList(posts, currentUserId), collectPage.getTotal(), page, size);
    }

    /**
     * 批量填充帖子 VO 列表
     * <p>统一查询作者信息、分类、当前用户的点赞/收藏状态，并将实体列表转换为 VO 列表。
     *
     * @param posts         帖子实体列表
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子 VO 列表
     */
    private List<PostVO> fillPostVOList(List<PostPO> posts, Long currentUserId) {
        Map<Long, UserProfileVO> userMap = userRemoteService.batchGetByIds(
                posts.stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        Map<Long, CategoryPO> categoryMap = categoryService.getByIds(extractCategoryIds(posts));
        Set<Long> likedPostIds = getLikedPostIds(currentUserId);
        Set<Long> collectedPostIds = getCollectedPostIds(currentUserId);
        return posts.stream()
                .map(po -> convertToVO(po, userMap, categoryMap, likedPostIds, collectedPostIds))
                .collect(Collectors.toList());
    }

    /**
     * 将帖子实体转换为视图对象
     * <p>作者信息和分类名称均从预查 map 中获取，避免 N+1 查询。
     *
     * @param po            帖子实体
     * @param userMap       预查的用户信息映射
     * @param categoryMap   预查的分类信息映射
     * @param likedPostIds  当前用户已点赞的帖子 ID 集合（可为空）
     * @param collectedPostIds 当前用户已收藏的帖子 ID 集合（可为空）
     * @return 帖子视图对象
     */
    private PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
                                Map<Long, CategoryPO> categoryMap, Set<Long> likedPostIds,
                                Set<Long> collectedPostIds) {
        PostVO vo = new PostVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setCategoryId(po.getCategoryId());
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setCover(po.getCover());
        if (StringUtils.hasText(po.getImages())) {
            vo.setImages(List.of(po.getImages().split(",")));
        } else {
            vo.setImages(Collections.emptyList());
        }
        if (StringUtils.hasText(po.getTags())) {
            vo.setTags(List.of(po.getTags().split(",")));
        } else {
            vo.setTags(Collections.emptyList());
        }
        vo.setStatus(po.getStatus());
        vo.setLikes(po.getLikes());
        vo.setLiked(likedPostIds.contains(po.getId()));
        vo.setComments(po.getComments());
        vo.setViews(po.getViews());
        vo.setCollections(po.getCollections());
        vo.setCollected(collectedPostIds.contains(po.getId()));
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());

        UserProfileVO author = userMap != null ? userMap.get(po.getUserId()) : null;
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        if (po.getCategoryId() != null) {
            CategoryPO category = categoryMap.get(po.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }

    /**
     * 从帖子列表中提取分类 ID 集合
     */
    private Set<Long> extractCategoryIds(List<PostPO> posts) {
        return posts.stream()
                .map(PostPO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 切换帖子点赞状态
     * <p>使用 post_like 表存储用户点赞关系（逻辑状态型）。
     * 不存在则插入 status=1，已存在则切换 status，同时原子更新 post.likes。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     * @return 更新后的点赞数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleLike(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 查询是否已存在点赞关系
        LambdaQueryWrapper<PostLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLikePO::getUserId, userId)
                .eq(PostLikePO::getPostId, postId);
        PostLikePO likePO = postLikeMapper.selectOne(wrapper);

        if (likePO == null) {
            // 不存在 → 插入 status=1
            PostLikePO newLike = new PostLikePO();
            newLike.setPostId(postId);
            newLike.setUserId(userId);
            newLike.setStatus(1);
            postLikeMapper.insert(newLike);
            postMapper.updateLikes(postId, 1);
            log.info("点赞帖子: postId={}, userId={}", postId, userId);
        } else if (likePO.getStatus() == 0) {
            // 已取消 → 恢复点赞
            likePO.setStatus(1);
            postLikeMapper.updateById(likePO);
            postMapper.updateLikes(postId, 1);
            log.info("点赞帖子(恢复): postId={}, userId={}", postId, userId);
        } else {
            // 已点赞 → 取消点赞
            likePO.setStatus(0);
            postLikeMapper.updateById(likePO);
            postMapper.updateLikes(postId, -1);
            log.info("取消点赞帖子: postId={}, userId={}", postId, userId);
        }

        // 重新查询最新点赞数
        PostPO updated = postMapper.selectById(postId);
        return updated.getLikes();
    }

    /**
     * 获取当前用户已点赞的帖子 ID 集合
     * <p>从 post_like 表批量查询当前用户 status=1 的点赞记录。
     * 注意：这里查的是全量，用于列表页批量回填 liked 状态。
     * 后续数据量增大时可改为按 postIds 批量查。
     *
     * @param userId 当前登录用户 ID（可为 null）
     * @return 已点赞帖子 ID 集合
     */
    private Set<Long> getLikedPostIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<PostLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLikePO::getUserId, userId)
                .eq(PostLikePO::getStatus, 1)
                .select(PostLikePO::getPostId);
        List<PostLikePO> list = postLikeMapper.selectList(wrapper);
        return list.stream()
                .map(PostLikePO::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 获取当前用户已收藏的帖子 ID 集合
     * <p>从 post_collect 表批量查询当前用户 status=1 的收藏记录。
     *
     * @param userId 当前登录用户 ID（可为 null）
     * @return 已收藏帖子 ID 集合
     */
    private Set<Long> getCollectedPostIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, userId)
                .eq(PostCollectPO::getStatus, 1)
                .select(PostCollectPO::getPostId);
        List<PostCollectPO> list = postCollectMapper.selectList(wrapper);
        return list.stream()
                .map(PostCollectPO::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 切换帖子收藏状态
     * <p>使用 post_collect 表存储用户收藏关系（逻辑状态型）。
     * 不存在则插入 status=1，已存在则切换 status，同时原子更新 post.collections。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     * @return 更新后的收藏数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleCollect(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 查询是否已存在收藏关系
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, userId)
                .eq(PostCollectPO::getPostId, postId);
        PostCollectPO collectPO = postCollectMapper.selectOne(wrapper);

        if (collectPO == null) {
            // 不存在 → 插入 status=1
            PostCollectPO newCollect = new PostCollectPO();
            newCollect.setPostId(postId);
            newCollect.setUserId(userId);
            newCollect.setStatus(1);
            postCollectMapper.insert(newCollect);
            postMapper.updateCollections(postId, 1);
            log.info("收藏帖子: postId={}, userId={}", postId, userId);
        } else if (collectPO.getStatus() == 0) {
            // 已取消 → 恢复收藏
            collectPO.setStatus(1);
            postCollectMapper.updateById(collectPO);
            postMapper.updateCollections(postId, 1);
            log.info("收藏帖子(恢复): postId={}, userId={}", postId, userId);
        } else {
            // 已收藏 → 取消收藏
            collectPO.setStatus(0);
            postCollectMapper.updateById(collectPO);
            postMapper.updateCollections(postId, -1);
            log.info("取消收藏帖子: postId={}, userId={}", postId, userId);
        }

        // 重新查询最新收藏数
        PostPO updated = postMapper.selectById(postId);
        return updated.getCollections();
    }

    /**
     * 记录浏览
     * <p>用户进入帖子详情页时调用。
     * <p>去重策略：登录用户按 userId、游客按 IP，30 分钟内同一标识只算一次。
     * 去重通过则向 Redis Hash {@code post:view:delta} 对应 field 原子 +1，
     * 由 {@link com.cyxz.post.task.ViewCountFlushTask} 定时刷库到 post.views。
     *
     * @param postId  帖子 ID
     * @param userId  当前登录用户 ID（可为 null，游客按 IP 去重）
     * @param request HTTP 请求（用于获取 IP）
     */
    @Override
    public void recordView(Long postId, Long userId, HttpServletRequest request) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || po.getStatus() != 1) {
            // 仅已发布帖子记录浏览
            return;
        }

        // 生成去重标识：登录用户用 userId，游客用 IP
        String identity = (userId != null) ? "user:" + userId : "ip:" + IpUtil.getClientIp(request);
        String dedupKey = CacheKeyConstants.POST_VIEW_DEDUP_PREFIX + postId + ":" + identity;

        // SETNX：key 不存在则设置成功（首次浏览），已存在则跳过
        Boolean firstView = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(CacheKeyConstants.POST_VIEW_DEDUP_MINUTES));

        if (Boolean.TRUE.equals(firstView)) {
            // 去重通过，Hash 增量 +1
            stringRedisTemplate.opsForHash()
                    .increment(CacheKeyConstants.POST_VIEW_DELTA, postId.toString(), 1);
        }
    }

    /**
     * SQL 聚合统计用户帖子数据
     * <p>通过一条 SQL 聚合查询当前用户所有已发布帖子的总浏览、总点赞、总收藏数，
     * 避免全量拉取到 Java 层求和，提升性能。
     *
     * @param userId 当前用户 ID
     * @return 统计数据（totalPosts, totalViews, totalLikes, totalCollections）
     */
    @Override
    public PostStatsVO getPostStats(Long userId) {
        PostStatsVO stats = postMapper.selectStatsByUserId(userId);
        if (stats == null) {
            stats = new PostStatsVO();
        }
        return stats;
    }

    /**
     * 查询用户作品排行榜（按浏览量倒序）
     * <p>用于数据中心展示用户浏览量最高的 N 个帖子。
     *
     * @param userId 当前用户 ID
     * @param limit  返回条数
     * @return 帖子 VO 列表
     */
    @Override
    public List<PostVO> getTopPosts(Long userId, int limit) {
        List<PostPO> topPosts = postMapper.selectTopPostsByViews(userId, limit);
        if (topPosts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userMap = userRemoteService.batchGetByIds(topPosts.stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        return topPosts.stream()
                .map(po -> convertToVO(po, userMap, Collections.emptyMap(), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    /**
     * 获取帖子作者 ID（内部接口）
     * <p>用于评论服务创建评论时通过 Feign 调用获取帖子作者。
     *
     * @param postId 帖子 ID
     * @return 作者用户 ID，帖子不存在返回 null
     */
    @Override
    public Long getPostAuthor(Long postId) {
        PostPO po = postMapper.selectById(postId);
        return po != null ? po.getUserId() : null;
    }

    /**
     * 获取帖子信息（内部接口）
     * <p>用于评论服务批量查询帖子标题。
     *
     * @param postId 帖子 ID
     * @return 帖子信息（postId, userId, title），帖子不存在返回空 Map
     */
    @Override
    public Map<String, Object> getPostInfo(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            return Collections.emptyMap();
        }
        return Map.of(
                "postId", po.getId(),
                "userId", po.getUserId(),
                "title", po.getTitle()
        );
    }

    /**
     * 批量获取帖子简要信息（内部接口）
     * <p>用于评论服务一次性查询多个帖子的标题，避免逐个 Feign 调用。
     *
     * @param postIds 帖子 ID 集合
     * @return 帖子信息列表（不存在的帖子不会出现在结果中）
     */
    @Override
    public List<PostInfoVO> batchGetPostInfo(Set<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostPO> posts = postMapper.selectBatchIds(postIds);
        return posts.stream().map(po -> {
            PostInfoVO vo = new PostInfoVO();
            vo.setPostId(po.getId());
            vo.setUserId(po.getUserId());
            vo.setTitle(po.getTitle());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询用户收到的点赞列表
     * <p>通过 JOIN post 表过滤出当前用户的帖子，再查 post_like 表中 status=1 的记录，
     * 并批量 Feign 查询点赞用户的昵称和头像。
     *
     * @param userId 当前用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果
     */
    @Override
    public PageResult<ReceivedLikeVO> getReceivedLikes(Long userId, int page, int size) {
        int total = postLikeMapper.countReceivedLikes(userId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<ReceivedLikeVO> records = postLikeMapper.selectReceivedLikes(userId, offset, size);

        // 批量查用户信息
        Set<Long> userIds = records.stream()
                .map(ReceivedLikeVO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserProfileVO> userMap = userRemoteService.batchGetByIds(userIds);
        records.forEach(vo -> {
            UserProfileVO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
        });

        return PageResult.of(records, total, page, size);
    }

}
