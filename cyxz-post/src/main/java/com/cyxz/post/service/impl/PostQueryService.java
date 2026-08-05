package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.utils.FeignResults;
import com.cyxz.post.utils.RequestContextUtil;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.entity.PostCollectPO;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.user.vo.UserProfileVO;
import com.cyxz.post.vo.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 帖子查询服务
 * <p>负责帖子详情、列表查询及 VO 填充。VO 填充与缓存清理方法对同包子 Service 开放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final UserFeignClient userFeignClient;
    private final CircleFeignClient circleFeignClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 帖子详情缓存 TTL（分钟），默认 30 */
    @Value("${spring.data.redis.cache-ttl-minutes:30}")
    private long cacheTtlMinutes;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("create_time", "views", "likes", "collections");

    /** 帖子是否仅作者可见 */
    private boolean isAuthorOnly(PostPO po) {
        return po != null && po.getStatus() != PostStatus.APPROVED;
    }

    /**
     * 根据 ID 查询帖子详情
     * <p>已发布帖子所有人可查看，草稿和已删除帖子仅作者本人可查看。
     */
    public PostVO getById(Long postId, Long currentUserId) {
        // 读缓存：游客和登录用户共用同一份缓存，liked/collected 在命中后补填
        String cacheKey = CacheKeyConstants.getPostDetailKey(postId);
        PostVO cached = (PostVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (currentUserId != null) {
                cached.setLiked(getLikedPostIds(currentUserId, Set.of(postId)).contains(postId));
                cached.setCollected(getCollectedPostIds(currentUserId, Set.of(postId)).contains(postId));
            }
            return cached;
        }

        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        // 非公开帖子仅作者本人可查看；非作者访问时按状态返回细分错误码
        if (isAuthorOnly(po) && !po.getUserId().equals(currentUserId)) {
            switch (po.getStatus()) {
                case PostStatus.DELETED:
                    throw new BusinessException(ErrorCode.POST_DELETED);
                case PostStatus.REJECTED:
                    throw new BusinessException(ErrorCode.POST_REJECTED);
                case PostStatus.PENDING:
                    throw new BusinessException(ErrorCode.POST_PENDING);
                default:
                    throw new BusinessException(ErrorCode.POST_NOT_INTERACTABLE);
            }
        }
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, Set.of(po.getUserId()));
        Map<Long, String> circleNameMap = extractCircleNameMap(List.of(po));
        Map<Long, String> sectionNameMap = extractSectionNameMap(List.of(po));
        Set<Long> likedPostIds = getLikedPostIds(currentUserId, Set.of(postId));
        Set<Long> collectedPostIds = getCollectedPostIds(currentUserId, Set.of(postId));
        PostVO vo = convertToVO(po, userMap, circleNameMap, sectionNameMap, likedPostIds, collectedPostIds);

        vo.setLiked(false);
        vo.setCollected(false);
        redisTemplate.opsForValue().set(cacheKey, vo, Duration.ofMinutes(cacheTtlMinutes));
        // 返回前补填当前用户的真实互动状态（缓存存的是无用户态的干净副本）
        if (currentUserId != null) {
            vo.setLiked(likedPostIds.contains(postId));
            vo.setCollected(collectedPostIds.contains(postId));
        }
        return vo;
    }

    /**
     * 分页查询帖子列表（仅已发布）
     */
    public PageResult<PostVO> listPosts(Long sectionId, Long circleId, String sortBy, int page, int size, Long currentUserId) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, PostStatus.APPROVED);
        if (sectionId != null) {
            wrapper.eq(PostPO::getSectionId, sectionId);
        }
        if (circleId != null) {
            wrapper.eq(PostPO::getCircleId, circleId);
        }
        // 排序：hot 按热度（点赞→评论→收藏→浏览），latest 及其他非法值按创建时间倒序
        if ("hot".equals(sortBy)) {
            wrapper.orderByDesc(PostPO::getLikes)
                   .orderByDesc(PostPO::getComments)
                   .orderByDesc(PostPO::getCollections)
                   .orderByDesc(PostPO::getViews)
                   .orderByDesc(PostPO::getCreateTime);
        } else {
            wrapper.orderByDesc(PostPO::getCreateTime);
        }
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        List<PostVO> vos = fillPostVOList(result.getRecords(), currentUserId);
        // 全局列表不应暴露个人置顶状态
        vos.forEach(vo -> vo.setPinned(false));
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 查询用户的帖子列表
     * <p>包含草稿、已发布和已删除，按创建时间倒序。
     */
    public PageResult<PostVO> listByUserId(Long userId, int page, int size, String sortField, String sortOrder) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, userId);

        // 回退字段用数据库列名 create_time（非驼峰）
        String field = (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField)) ? sortField : "create_time";
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.last("ORDER BY is_pinned DESC, pinned_time DESC, " + field + (asc ? " ASC" : " DESC"));

        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, result.getRecords().stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> circleNameMap = extractCircleNameMap(result.getRecords());
        Map<Long, String> sectionNameMap = extractSectionNameMap(result.getRecords());
        Set<Long> postIds = result.getRecords().stream().map(PostPO::getId).collect(Collectors.toSet());
        Set<Long> likedPostIds = getLikedPostIds(userId, postIds);
        Set<Long> collectedPostIds = getCollectedPostIds(userId, postIds);

        List<PostVO> vos = result.getRecords().stream()
                .map(po -> convertToVO(po, userMap, circleNameMap, sectionNameMap, likedPostIds, collectedPostIds))
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 查询指定用户的已发布帖子列表
     */
    public PageResult<PostVO> listByTargetUserId(Long targetUserId, Long currentUserId, int page, int size) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, targetUserId);
        wrapper.eq(PostPO::getStatus, PostStatus.APPROVED);
        wrapper.last("ORDER BY is_pinned DESC, pinned_time DESC, create_time DESC");
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        return PageResult.of(fillPostVOList(result.getRecords(), currentUserId), result.getTotal(), page, size);
    }

    /**
     * 查询指定用户的收藏帖子列表
     */
    public PageResult<PostVO> listFavorites(Long targetUserId, Long currentUserId, int page, int size) {
        // 从 post_collect 表获取目标用户收藏的帖子 ID（仅 CommonStatus.ACTIVE）
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, targetUserId)
                .eq(PostCollectPO::getStatus, CommonStatus.ACTIVE)
                .orderByDesc(PostCollectPO::getCreateTime);
        Page<PostCollectPO> pageParam = PageConstants.pageOf(page, size);
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
                .filter(po -> po.getStatus() == PostStatus.APPROVED)
                .collect(Collectors.toList());

        return PageResult.of(fillPostVOList(posts, currentUserId), collectPage.getTotal(), page, size);
    }

    /**
     * 分页查询关注动态（已加入圈子的帖子）
     */
    public PageResult<PostVO> listFollowingPosts(Long userId, int page, int size) {
        java.util.List<Long> followingUserIds = FeignResults.unwrapOrEmpty(userFeignClient.getFollowingUserIds(userId));
        if (followingUserIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, PostStatus.APPROVED);
        wrapper.in(PostPO::getUserId, followingUserIds);
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        List<PostVO> vos = fillPostVOList(result.getRecords(), userId);
        vos.forEach(vo -> vo.setPinned(false));
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 分页查询待审核帖子列表
     */
    public PageResult<PostVO> listPendingReview(int page, int size) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, PostStatus.PENDING);
        wrapper.orderByAsc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);
        List<PostVO> vos = fillPostVOList(result.getRecords(), null);
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 批量填充帖子 VO 列表
     * <p>并行查询用户信息、板块、圈子、点赞/收藏状态，减少串行等待时间。
     */
    public List<PostVO> fillPostVOList(List<PostPO> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = posts.stream().map(PostPO::getUserId).collect(Collectors.toSet());
        Set<Long> postIds = posts.stream().map(PostPO::getId).collect(Collectors.toSet());

        CompletableFuture<Map<Long, UserProfileVO>> userFuture =
                CompletableFuture.supplyAsync(RequestContextUtil.wrap(() -> UserFeignHelper.batchGetUsers(userFeignClient, userIds)));
        CompletableFuture<Map<Long, String>> circleFuture =
                CompletableFuture.supplyAsync(RequestContextUtil.wrap(() -> extractCircleNameMap(posts)));
        CompletableFuture<Map<Long, String>> sectionFuture =
                CompletableFuture.supplyAsync(RequestContextUtil.wrap(() -> extractSectionNameMap(posts)));
        CompletableFuture<Set<Long>> likedIdFuture =
                CompletableFuture.supplyAsync(() -> getLikedPostIds(currentUserId, postIds));
        CompletableFuture<Set<Long>> collectedIdFuture =
                CompletableFuture.supplyAsync(() -> getCollectedPostIds(currentUserId, postIds));

        CompletableFuture.allOf(userFuture, circleFuture, sectionFuture, likedIdFuture, collectedIdFuture).join();

        Map<Long, UserProfileVO> userMap = userFuture.join();
        Map<Long, String> circleNameMap = circleFuture.join();
        Map<Long, String> sectionNameMap = sectionFuture.join();
        Set<Long> likedPostIds = likedIdFuture.join();
        Set<Long> collectedPostIds = collectedIdFuture.join();

        return posts.stream()
                .map(po -> convertToVO(po, userMap, circleNameMap, sectionNameMap, likedPostIds, collectedPostIds))
                .collect(Collectors.toList());
    }

    /**
     * 将帖子实体转换为视图对象（无板块名称）
     */
    public PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
                                Map<Long, String> circleNameMap,
                                Set<Long> likedPostIds, Set<Long> collectedPostIds) {
        return convertToVO(po, userMap, circleNameMap, Collections.emptyMap(), likedPostIds, collectedPostIds);
    }

    /**
     * 将帖子实体转换为视图对象
     * <p>作者信息和板块名称均从预查 map 中获取，避免 N+1 查询。
     */
    public PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
                                Map<Long, String> circleNameMap,
                                Map<Long, String> sectionNameMap,
                                Set<Long> likedPostIds, Set<Long> collectedPostIds) {
        PostVO vo = new PostVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setPostType(po.getPostType() != null ? po.getPostType() : "NORMAL");
        vo.setCircleId(po.getCircleId());
        vo.setSectionId(po.getSectionId());
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
        vo.setReviewReason(po.getReviewReason());
        vo.setLikes(po.getLikes());
        vo.setLiked(likedPostIds.contains(po.getId()));
        vo.setComments(po.getComments());
        vo.setViews(po.getViews());
        vo.setCollections(po.getCollections());
        vo.setCollected(collectedPostIds.contains(po.getId()));
        vo.setPinned(po.getIsPinned() != null && po.getIsPinned() == 1);
        vo.setPinnedTime(po.getPinnedTime());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());

        UserProfileVO author = userMap != null ? userMap.get(po.getUserId()) : null;
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        if (po.getCircleId() != null && circleNameMap != null) {
            String circleName = circleNameMap.get(po.getCircleId());
            if (circleName != null) {
                vo.setCircleName(circleName);
            }
        }

        if (po.getSectionId() != null && sectionNameMap != null) {
            String sectionName = sectionNameMap.get(po.getSectionId());
            if (sectionName != null) {
                vo.setSectionName(sectionName);
            }
        }

        return vo;
    }

    /**
     * 从帖子列表中提取圈子 ID 集合并批量查名称
     */
    public Map<Long, String> extractCircleNameMap(List<PostPO> posts) {
        Set<Long> circleIds = posts.stream()
                .map(PostPO::getCircleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return FeignResults.unwrapOrEmptyMap(circleFeignClient.batchGetNames(circleIds));
    }

    /**
     * 从帖子列表中提取板块 ID 集合并批量查名称
     */
    public Map<Long, String> extractSectionNameMap(List<PostPO> posts) {
        Set<Long> sectionIds = posts.stream()
                .map(PostPO::getSectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sectionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return FeignResults.unwrapOrEmptyMap(circleFeignClient.batchGetSectionNames(sectionIds));
    }

    /**
     * 获取当前用户对指定帖子的点赞状态
     */
    private Set<Long> getLikedPostIds(Long userId, Set<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<PostLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLikePO::getUserId, userId)
                .in(PostLikePO::getPostId, postIds)
                .eq(PostLikePO::getStatus, CommonStatus.ACTIVE)
                .select(PostLikePO::getPostId);
        List<PostLikePO> list = postLikeMapper.selectList(wrapper);
        return list.stream()
                .map(PostLikePO::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 获取当前用户对指定帖子的收藏状态
     */
    private Set<Long> getCollectedPostIds(Long userId, Set<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, userId)
                .in(PostCollectPO::getPostId, postIds)
                .eq(PostCollectPO::getStatus, CommonStatus.ACTIVE)
                .select(PostCollectPO::getPostId);
        List<PostCollectPO> list = postCollectMapper.selectList(wrapper);
        return list.stream()
                .map(PostCollectPO::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 清除帖子详情缓存
     * <p>帖子更新/删除时调用，下次请求会从 DB 重新加载并写入缓存。
     */
    public void evictDetailCache(Long postId) {
        String key = CacheKeyConstants.getPostDetailKey(postId);
        redisTemplate.delete(key);
    }
}
