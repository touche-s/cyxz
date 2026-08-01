package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.message.api.constant.NotificationConstants;
import com.cyxz.message.api.event.NotificationEvent;
import com.cyxz.post.service.AiReviewService;
import com.cyxz.post.service.AiReviewService.AiReviewResult;
import com.cyxz.post.service.SensitiveWordService;
import com.cyxz.post.vo.*;
import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.entity.PostCollectPO;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.PostService;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final CircleFeignClient circleFeignClient;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final CommentFeignClient commentFeignClient;
    private final UserFeignClient userFeignClient;
    private final SensitiveWordService sensitiveWordService;
    private final AiReviewService aiReviewService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    /** 帖子详情缓存 TTL（分钟），默认 30 */
    @Value("${spring.data.redis.cache-ttl-minutes:30}")
    private long cacheTtlMinutes;

    // ==================== 帖子状态常量与流转规则 ====================
    // 0=草稿  1=待审核  2=已通过(公开)  3=拒绝  4=已删除
    // 流转链：草稿→待审→通过/拒绝；通过→删除；拒绝→草稿(重编重新发布)

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_DELETED = 4;

    /** 合法的状态流转表：当前状态 → 允许迁入的目标状态列表 */
    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS = Map.of(
            STATUS_DRAFT, Set.of(STATUS_PENDING, STATUS_DELETED),
            STATUS_PENDING, Set.of(STATUS_APPROVED, STATUS_REJECTED, STATUS_DRAFT),
            STATUS_APPROVED, Set.of(STATUS_DELETED),
            STATUS_REJECTED, Set.of(STATUS_DRAFT),
            STATUS_DELETED, Set.of(STATUS_DRAFT)
    );

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("create_time", "views", "likes", "collections");

    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            STATUS_DRAFT, "草稿",
            STATUS_PENDING, "待审核",
            STATUS_APPROVED, "已通过",
            STATUS_REJECTED, "拒绝",
            STATUS_DELETED, "已删除"
    );

    private boolean canTransition(int from, int to) {
        if (from == to) return true;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    private String statusLabel(int status) {
        return STATUS_LABEL.getOrDefault(status, "未知");
    }

    /** 帖子是否公开可见（非作者也可看）—— status=2 已通过 */
    private boolean isPublicVisible(PostPO po) {
        return po != null && po.getStatus() == STATUS_APPROVED;
    }

    /** 帖子是否仅作者可见 */
    private boolean isAuthorOnly(PostPO po) {
        return po != null && po.getStatus() != STATUS_APPROVED;
    }

    /** 帖子是否允许互动（点赞、收藏、评论） */
    private boolean isInteractable(PostPO po) {
        return isPublicVisible(po);
    }

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
        if (request.getStatus() != null && request.getStatus() == 1) {
            validatePublishFields(request.getTitle(),
                    request.getCircleId(), request.getContent(), request.getImages(), request.getPostType(), userId);
            // 发布时检测敏感词，命中直接拒绝
            Set<String> hits = sensitiveWordService.check(request.getTitle(), request.getContent());
            if (!hits.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "内容包含敏感词：" + String.join("、", hits));
            }
        } else {
            validateDraftHasContent(request);
        }
        PostPO po = new PostPO();
        po.setUserId(userId);
        po.setCircleId(request.getCircleId());
        po.setSectionId(request.getSectionId());
        po.setTitle(request.getTitle());
        po.setContent(request.getContent());
        po.setCover(request.getCover());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            po.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            po.setTags(String.join(",", request.getTags()));
        }
        String postType = request.getPostType() != null ? request.getPostType() : "NORMAL";
        po.setPostType(postType);
        po.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        po.setLikes(0);
        po.setComments(0);
        po.setViews(0);
        po.setCollections(0);
        postMapper.insert(po);
        if (po.getStatus() == STATUS_PENDING) {
            // 异步调 AI 审核，审核结果在本服务内处理
            final Long postId = po.getId();
            final String postTitle = po.getTitle();
            final String postContent = po.getContent();
            final List<String> images = request.getImages();
            CompletableFuture.runAsync(() -> {
                try {
                    AiReviewResult result = aiReviewService.review(postId, postTitle, postContent, images);
                    handleReviewResult(postId, userId, postTitle, result);
                } catch (Exception e) {
                    log.error("AI 审核调用失败，转人工审核: postId={}, error={}", postId, e.getMessage());
                }
            });
        }
        log.info("创建帖子成功: postId={}, userId={}", po.getId(), userId);
        return po.getId();
    }

    /**
     * 更新帖子
     * <p>支持三种业务动作：
     * <ul>
     *   <li>保存草稿：仅更新内容字段，不触发发布校验</li>
     *   <li>草稿转发布 / 更新已发布：更新内容 + status=1，强制执行发布完整性校验</li>
     *   <li>状态迁移（转草稿、恢复）：仅变更 status 字段</li>
     * </ul>
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


        // 发布动作：仅校验请求体完整性，前端必须传完整发布数据
        boolean isPublishAction = request.getStatus() != null && request.getStatus() == 1;
        if (isPublishAction) {
            Long targetCircleId = request.getCircleId() != null ? request.getCircleId() : po.getCircleId();
            validatePublishFields(
                    request.getTitle(),
                    targetCircleId,
                    request.getContent(),
                    request.getImages(),
                    request.getPostType(),
                    userId
            );
            // 敏感词检测
            Set<String> hits = sensitiveWordService.check(request.getTitle(), request.getContent());
            if (!hits.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "内容包含敏感词：" + String.join("、", hits));
            }
        }

        applyContentUpdate(po, request);

        if (request.getStatus() != null) {
            if (!canTransition(po.getStatus(), request.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "不允许从 " + statusLabel(po.getStatus()) + " 直接变更为 " + statusLabel(request.getStatus()));
            }
            po.setStatus(request.getStatus());
        }

        postMapper.updateById(po);
        evictDetailCache(po.getId());
        syncPostToEs(po);

        log.info("{}帖子成功: postId={}, userId={}", isPublishAction ? "发布" : "更新", po.getId(), userId);
    }

    /**
     * 将请求中的非 null 字段应用到实体
     */
    private void applyContentUpdate(PostPO po, UpdatePostRequest request) {
        if (request.getPostType() != null) po.setPostType(request.getPostType());
        if (request.getCircleId() != null) po.setCircleId(request.getCircleId());
        if (request.getSectionId() != null) po.setSectionId(request.getSectionId());
        if (StringUtils.hasText(request.getTitle())) po.setTitle(request.getTitle());
        if (request.getContent() != null) po.setContent(request.getContent());
        if (request.getCover() != null) po.setCover(request.getCover());
        if (request.getImages() != null) po.setImages(String.join(",", request.getImages()));
        if (request.getTags() != null) po.setTags(String.join(",", request.getTags()));
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
        po.setStatus(STATUS_DELETED);
        postMapper.updateById(po);
        evictDetailCache(postId);
        syncPostToEs(po);
        log.info("软删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 彻底删除帖子（物理删除 + 级联清理关联数据）
     * <p>仅允许删除 status=2 的帖子（已在回收站中），
     * 同时通过 Feign 清理评论和评论点赞，本地清理帖子点赞、帖子收藏。
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeletePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (po.getStatus() != STATUS_DELETED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅回收站中的帖子可彻底删除");
        }

        // 1. 删除帖子下的评论和评论点赞（Feign 调 comment 服务）
        Result<Void> commentResult = commentFeignClient.deleteByPostId(postId);
        if (commentResult == null || !commentResult.isSuccess()) {
            log.warn("删除帖子关联评论失败: postId={}, result={}", postId, commentResult);
        }

        // 2. 删除帖子点赞
        postLikeMapper.delete(
                new LambdaQueryWrapper<PostLikePO>()
                    .eq(PostLikePO::getPostId, postId));

        // 3. 删除帖子收藏
        postCollectMapper.delete(
                new LambdaQueryWrapper<PostCollectPO>()
                    .eq(PostCollectPO::getPostId, postId));

        // 4. 删除帖子主表
        postMapper.deleteById(postId);
        evictDetailCache(postId);
        // ES 同步删除
        syncPostToEsDelete(postId);
        log.info("彻底删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 根据 ID 查询帖子详情
     * <p>已发布帖子所有人可查看，草稿和已删除帖子仅作者本人可查看。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子视图对象（含作者信息、板块名称）
     */
    @Override
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
        // 非公开帖子仅作者本人可查看
        if (isAuthorOnly(po) && !po.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, Set.of(po.getUserId()));
        Map<Long, String> circleNameMap = extractCircleNameMap(List.of(po));
        Map<Long, String> sectionNameMap = extractSectionNameMap(List.of(po));
        Set<Long> likedPostIds = getLikedPostIds(currentUserId, Set.of(postId));
        Set<Long> collectedPostIds = getCollectedPostIds(currentUserId, Set.of(postId));
        PostVO vo = convertToVO(po, userMap, circleNameMap, sectionNameMap, likedPostIds, collectedPostIds);

        redisTemplate.opsForValue().set(cacheKey, vo, Duration.ofMinutes(cacheTtlMinutes));
        return vo;
    }

    /**
     * 分页查询帖子列表（仅已发布）
     * <p>支持按板块、圈子筛选和排序。
     *
     * @param sectionId    板块 ID（可为 null，null 时查全部板块）
     * @param circleId      圈子 ID（可为 null）
     * @param sortBy        排序方式：latest 按创建时间倒序，hot 按热度倒序，非法值降级为 latest
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listPosts(Long sectionId, Long circleId, String sortBy, int page, int size, Long currentUserId) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, STATUS_APPROVED);
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
     *
     * @param userId 用户 ID
     * @param sortField 排序字段（createTime/views/likes/collections），默认 createTime
     * @param sortOrder 排序方向（asc/desc），默认 desc
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listByUserId(Long userId, int page, int size, String sortField, String sortOrder) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, userId);

        String field = (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField)) ? sortField : "createTime";
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
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, targetUserId);
        wrapper.eq(PostPO::getStatus, STATUS_APPROVED);
        wrapper.last("ORDER BY is_pinned DESC, pinned_time DESC, create_time DESC");
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
                .filter(po -> po.getStatus() == STATUS_APPROVED)
                .collect(Collectors.toList());

        return PageResult.of(fillPostVOList(posts, currentUserId), collectPage.getTotal(), page, size);
    }

    /**
     * 批量填充帖子 VO 列表
     * <p>并行查询用户信息、板块、圈子、点赞/收藏状态，减少串行等待时间。
     *
     * @param posts         帖子实体列表
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子 VO 列表
     */
    private List<PostVO> fillPostVOList(List<PostPO> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = posts.stream().map(PostPO::getUserId).collect(Collectors.toSet());
        Set<Long> postIds = posts.stream().map(PostPO::getId).collect(Collectors.toSet());

        CompletableFuture<Map<Long, UserProfileVO>> userFuture =
                CompletableFuture.supplyAsync(() -> UserFeignHelper.batchGetUsers(userFeignClient, userIds));
        CompletableFuture<Map<Long, String>> circleFuture =
                CompletableFuture.supplyAsync(() -> extractCircleNameMap(posts));
        CompletableFuture<Map<Long, String>> sectionFuture =
                CompletableFuture.supplyAsync(() -> extractSectionNameMap(posts));
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
     * 将帖子实体转换为视图对象
     * <p>作者信息和板块名称均从预查 map 中获取，避免 N+1 查询。
     *
     * @param po              帖子实体
     * @param userMap         预查的用户信息映射
     * @param circleNameMap   预查的圈子名称映射
     * @param likedPostIds    当前用户已点赞的帖子 ID 集合（可为空）
     * @param collectedPostIds 当前用户已收藏的帖子 ID 集合（可为空）
     * @return 帖子视图对象
     */
    private PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
                                Map<Long, String> circleNameMap,
                                Set<Long> likedPostIds, Set<Long> collectedPostIds) {
        return convertToVO(po, userMap, circleNameMap, Collections.emptyMap(), likedPostIds, collectedPostIds);
    }

    private PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
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
    private Map<Long, String> extractCircleNameMap(List<PostPO> posts) {
        Set<Long> circleIds = posts.stream()
                .map(PostPO::getCircleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<Map<Long, String>> result = circleFeignClient.batchGetNames(circleIds);
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        return result.getData();
    }


    /**
     * 从帖子列表中提取板块 ID 集合并批量查名称
     */
    private Map<Long, String> extractSectionNameMap(List<PostPO> posts) {
        Set<Long> sectionIds = posts.stream()
                .map(PostPO::getSectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sectionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<Map<Long, String>> result = circleFeignClient.batchGetSectionNames(sectionIds);
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        return result.getData();
    }


    /**
     * 获取当前用户对指定帖子的点赞状态
     * <p>按需查询，只查当前页涉及的 postIds，避免全量扫描。
     *
     * @param userId  当前登录用户 ID（可为 null）
     * @param postIds 待查询的帖子 ID 集合
     * @return 已点赞帖子 ID 集合
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
     * <p>按需查询，只查当前页涉及的 postIds，避免全量扫描。
     *
     * @param userId  当前登录用户 ID（可为 null）
     * @param postIds 待查询的帖子 ID 集合
     * @return 已收藏帖子 ID 集合
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
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, topPosts.stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> circleNameMap = extractCircleNameMap(topPosts);
        Map<Long, String> sectionNameMap = extractSectionNameMap(topPosts);
        return topPosts.stream()
                .map(po -> convertToVO(po, userMap, circleNameMap, sectionNameMap, Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    /**
     * 获取今日新增互动统计
     * <p>一条 SQL 查询今日该用户帖子收到的新点赞、新收藏、新评论数。
     *
     * @param userId 当前用户 ID
     * @return 今日统计 VO
     */
    @Override
    public TodayStatsVO getTodayStats(Long userId) {
        TodayStatsVO stats = postMapper.selectTodayStats(userId);
        if (stats == null) {
            stats = new TodayStatsVO(0, 0, 0);
        }
        try {
            Result<Integer> result = commentFeignClient.countTodayComments(userId);
            if (result != null && result.getData() != null) {
                stats.setTodayComments(result.getData());
            }
        } catch (Exception e) {
            log.warn("获取今日评论数失败: userId={}", userId, e);
        }
        return stats;
    }

    /**
     * 获取数据中心仪表盘数据
     * <p>并行查询概览统计、月度趋势、板块分布和 Top 5 作品，
     * 组装为 DashboardVO 返回。
     *
     * @param userId 当前用户 ID
     * @return 仪表盘 VO
     */
    @Override
    public DashboardVO getDashboard(Long userId) {
        PostStatsVO summary = postMapper.selectStatsByUserId(userId);
        if (summary == null) {
            summary = new PostStatsVO();
        }
        List<DashboardVO.MonthlyTrendVO> trends = postMapper.selectMonthlyTrends(userId);
        if (trends == null) {
            trends = Collections.emptyList();
        }
        List<DashboardVO.DailyTrendVO> dailyTrends = postMapper.selectDailyTrends(userId);
        if (dailyTrends == null) {
            dailyTrends = Collections.emptyList();
        }
        List<PostVO> topPosts = getTopPosts(userId, 5);

        DashboardVO dashboard = new DashboardVO();
        dashboard.setSummary(summary);
        dashboard.setMonthlyTrends(trends);
        dashboard.setDailyTrends(dailyTrends);
        dashboard.setSectionDistribution(Collections.emptyList());
        dashboard.setTopPosts(topPosts);
        return dashboard;
    }

    /**
     * 获取帖子作者 ID（内部接口）
     * <p>用于评论服务创建评论时通过 Feign 调用获取帖子作者。
     * 仅已发布帖子可评论，草稿和已删除帖子拒绝。
     *
     * @param postId 帖子 ID
     * @return 作者用户 ID
     * @throws BusinessException 帖子不存在或非已发布状态
     */
    @Override
    public Long getPostAuthor(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return po.getUserId();
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
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);
        records.forEach(vo -> {
            UserProfileVO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
        });

        return PageResult.of(records, total, page, size);
    }

    /**
     * 搜索帖子（标题+内容模糊匹配）
     * <p>仅搜索已发布帖子，按创建时间倒序。
     */
    @Override
    public PageResult<PostVO> searchPosts(String keyword, int page, int size, Long currentUserId) {
        if (keyword == null || keyword.isBlank()) {
            return PageResult.empty(page, size);
        }
        String likeKeyword = "%" + keyword.trim() + "%";
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, STATUS_APPROVED)
                .and(w -> w.like(PostPO::getTitle, likeKeyword).or().like(PostPO::getContent, likeKeyword));
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);
        return PageResult.of(fillPostVOList(result.getRecords(), currentUserId), result.getTotal(), page, size);
    }

    /**
     * 校验发布必填项：标题、板块、正文、图片 + 圈子发布权限 Feign 校验
     */
    private void validatePublishFields(String title, Long circleId, String content, List<String> images, String postType, Long userId) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "发布时标题不能为空");
        }
        if (circleId == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "发布时圈子不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "发布时正文不能为空");
        }
        if (!"ARTICLE".equals(postType) && (images == null || images.isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "图文帖发布时至少需要一张图片");
        }
        Result<Map<String, Object>> r = circleFeignClient.checkPublishable(circleId, userId);
        Map<String, Object> data = r != null ? r.getData() : null;
        if (data == null || !Boolean.TRUE.equals(data.get("publishable"))) {
            if (data != null && !Boolean.TRUE.equals(data.get("exists"))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "圈子不存在");
            }
            if (data != null && !Boolean.TRUE.equals(data.get("enabled"))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该圈子已停用，暂不可发布");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先加入该圈子再发布");
        }
    }

    /**
     * 校验草稿至少有一项内容
     */
    private void validateDraftHasContent(CreatePostRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "草稿标题不能为空");
        }
        if (request.getCircleId() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "草稿圈子不能为空");
        }
        boolean hasContent = (request.getContent() != null && !request.getContent().isBlank())
                || (request.getImages() != null && !request.getImages().isEmpty());
        if (!hasContent) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "草稿至少需要正文或图片");
        }
    }

    @Override
    public void pinPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (po.getStatus() != STATUS_APPROVED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已发布帖子可置顶");
        }
        int pinnedCount = postMapper.countPinnedPosts(userId);
        if (pinnedCount >= 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最多置顶 3 条帖子");
        }
        postMapper.pinPost(userId, postId);
        log.info("置顶帖子成功: postId={}, userId={}", postId, userId);
    }

    @Override
    public void unpinPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        postMapper.unpinPost(userId, postId);
        log.info("取消置顶帖子成功: postId={}, userId={}", postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchOperate(Long userId, List<Long> postIds, String action) {
        if (postIds == null || postIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要操作的帖子");
        }
        if ("publish".equals(action)) {
            for (Long postId : postIds) {
                PostPO po = postMapper.selectById(postId);
                if (po == null || !po.getUserId().equals(userId)) continue;
                if (po.getStatus() != STATUS_DRAFT) continue;
                // 草稿转发布需校验必填字段
                if (po.getTitle() == null || po.getTitle().isBlank()
                        || po.getContent() == null || po.getContent().isBlank()
                        || po.getImages() == null || po.getImages().isBlank()) {
                    log.warn("草稿缺少必填字段，跳过: postId={}", postId);
                    continue;
                }
                // 异步 AI 审核
                final Long pid = postId;
                final String title = po.getTitle();
                final String content = po.getContent();
                List<String> imgs = po.getImages() != null && !po.getImages().isBlank()
                        ? Arrays.asList(po.getImages().split(",")) : Collections.emptyList();
                CompletableFuture.runAsync(() -> {
                    try {
                        AiReviewResult result = aiReviewService.review(pid, title, content, imgs);
                        handleReviewResult(pid, userId, title, result);
                    } catch (Exception e) {
                        log.error("批量发布 AI 审核失败: postId={}, error={}", pid, e.getMessage());
                    }
                });
            }
            postMapper.batchUpdateStatus(userId, postIds, STATUS_PENDING);
        } else if ("delete".equals(action)) {
            postMapper.batchUpdateStatus(userId, postIds, STATUS_DELETED);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的操作类型: " + action);
        }
        log.info("批量操作完成: action={}, count={}, userId={}", action, postIds.size(), userId);
    }

    @Override
    public PageResult<PostVO> listFollowingPosts(Long userId, int page, int size) {
        Result<List<Long>> feignResult = userFeignClient.getFollowingUserIds(userId);
        List<Long> followingUserIds = (feignResult != null && feignResult.getData() != null)
                ? feignResult.getData() : Collections.emptyList();

        if (followingUserIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, STATUS_APPROVED);
        wrapper.in(PostPO::getUserId, followingUserIds);
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        List<PostVO> vos = fillPostVOList(result.getRecords(), userId);
        vos.forEach(vo -> vo.setPinned(false));
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 清除帖子详情缓存
     * <p>帖子更新/删除时调用，下次请求会从 DB 重新加载并写入缓存。
     */
    private void evictDetailCache(Long postId) {
        String key = CacheKeyConstants.getPostDetailKey(postId);
        redisTemplate.delete(key);
    }

    @Override
    public void approvePost(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (po.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子不在待审核状态");
        }
        if (!canTransition(po.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + statusLabel(po.getStatus()) + " 直接变更为 " + statusLabel(STATUS_APPROVED));
        }
        po.setStatus(STATUS_APPROVED);
        po.setReviewReason(null);
        postMapper.updateById(po);
        evictDetailCache(postId);
        log.info("帖子审核通过: postId={}", postId);
    }

    @Override
    public void rejectPost(Long postId, String reason) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (po.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子不在待审核状态");
        }
        if (!canTransition(po.getStatus(), STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + statusLabel(po.getStatus()) + " 直接变更为 " + statusLabel(STATUS_REJECTED));
        }
        po.setStatus(STATUS_REJECTED);
        po.setReviewReason(reason);
        postMapper.updateById(po);
        evictDetailCache(postId);
        log.info("帖子审核拒绝: postId={}, reason={}", postId, reason);
    }

    /**
     * 处理 AI 审核结果
     * <p>通过：更新状态为已通过，清除缓存，发通知
     * <p>拒绝：更新状态为拒绝并记录原因，清除缓存，发通知
     */
    private void handleReviewResult(Long postId, Long authorId, String title, AiReviewResult result) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || po.getStatus() != STATUS_PENDING) {
            log.info("帖子 {} 状态已变更，跳过审核结果处理", postId);
            return;
        }
        if (result.isPassed()) {
            po.setStatus(STATUS_APPROVED);
            po.setReviewReason(null);
            postMapper.updateById(po);
            evictDetailCache(postId);
            syncPostToEs(po);
            sendReviewNotify(authorId, "POST_APPROVED", null, postId, title);
            log.info("AI 审核通过: postId={}", postId);
        } else {
            po.setStatus(STATUS_REJECTED);
            po.setReviewReason(result.getReason());
            postMapper.updateById(po);
            evictDetailCache(postId);
            sendReviewNotify(authorId, "POST_REJECTED", result.getReason(), postId, title);
            log.warn("AI 审核拒绝: postId={}, reason={}", postId, result.getReason());
        }
    }

    /**
     * 同步帖子到 ES：APPROVED 状态写入，其他状态删除
     */
    private void syncPostToEs(PostPO po) {
        try {
            String action = po.getStatus() != null && po.getStatus() == STATUS_APPROVED ? "CREATE" : "DELETE";
            PostEsSyncEvent event = buildSyncEvent(po, action);
            rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("ES 同步消息发送失败: postId={}", po.getId(), e);
        }
    }

    private void syncPostToEsDelete(Long postId) {
        try {
            PostEsSyncEvent event = PostEsSyncEvent.builder()
                    .action("DELETE")
                    .postId(postId)
                    .build();
            rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("ES 同步删除消息发送失败: postId={}", postId, e);
        }
    }

    private PostEsSyncEvent buildSyncEvent(PostPO po, String action) {
        return PostEsSyncEvent.builder()
                .action(action)
                .postId(po.getId())
                .userId(po.getUserId())
                .circleId(po.getCircleId())
                .sectionId(po.getSectionId())
                .postType(po.getPostType())
                .title(po.getTitle())
                .content(po.getContent())
                .cover(po.getCover())
                .tags(po.getTags())
                .status(po.getStatus())
                .likes(po.getLikes())
                .comments(po.getComments())
                .views(po.getViews())
                .collections(po.getCollections())
                .createTime(po.getCreateTime() != null
                        ? po.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : System.currentTimeMillis())
                .build();
    }

    private void sendReviewNotify(Long receiverId, String type, String reason, Long postId, String title) {
        try {
            String content = "POST_APPROVED".equals(type)
                    ? "你的帖子《" + title + "》审核通过，已公开发布"
                    : "你的帖子《" + title + "》未通过审核" + (reason != null ? "：" + reason : "");
            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(receiverId)
                    .senderId(0L)
                    .type(type)
                    .title("审核结果")
                    .content(content)
                    .targetType("post")
                    .targetId(postId)
                    .createTime(System.currentTimeMillis())
                    .build();
            rabbitTemplate.convertAndSend(NotificationConstants.EXCHANGE, NotificationConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("发送审核通知失败: postId={}, receiverId={}", postId, receiverId, e);
        }
    }

    @Override
    public PageResult<PostVO> listPendingReview(int page, int size) {
        Page<PostPO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, STATUS_PENDING);
        wrapper.orderByAsc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);
        List<PostVO> vos = fillPostVOList(result.getRecords(), null);
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public Map<Long, Integer> batchCountByCircle(Set<Long> circleIds) {
        if (circleIds == null || circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = postMapper.batchCountByCircleIds(circleIds);
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long circleId = ((Number) row.get("circle_id")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            result.put(circleId, cnt);
        }
        // 没有帖子的圈子返回 0
        for (Long circleId : circleIds) {
            result.putIfAbsent(circleId, 0);
        }
        return result;
    }

}
