package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PostCountConstants;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.constant.PostType;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.entity.PostCollectPO;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.AiReviewService;
import com.cyxz.post.service.AiReviewService.AiReviewResult;
import com.cyxz.post.service.SensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 帖子写操作服务
 * <p>负责帖子的创建、更新、删除、置顶、批量操作，依赖 EsSyncService 同步索引、
 * ReviewService 处理异步 AI 审核结果、QueryService 清理缓存。
 * <p>AI 审核走 {@code aiReviewExecutor} 慢任务池，避免拖累 {@link PostQueryService} 的并行查询。
 */
@Slf4j
@Service
public class PostCommandService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final CommentFeignClient commentFeignClient;
    private final CircleFeignClient circleFeignClient;
    private final SensitiveWordService sensitiveWordService;
    private final AiReviewService aiReviewService;
    private final PostEsSyncService postEsSyncService;
    private final PostReviewService postReviewService;
    private final PostQueryService postQueryService;
    private final ExecutorService aiReviewExecutor;

    public PostCommandService(PostMapper postMapper,
                              PostLikeMapper postLikeMapper,
                              PostCollectMapper postCollectMapper,
                              CommentFeignClient commentFeignClient,
                              CircleFeignClient circleFeignClient,
                              SensitiveWordService sensitiveWordService,
                              AiReviewService aiReviewService,
                              PostEsSyncService postEsSyncService,
                              PostReviewService postReviewService,
                              PostQueryService postQueryService,
                              @Qualifier("aiReviewExecutor") ExecutorService aiReviewExecutor) {
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
        this.postCollectMapper = postCollectMapper;
        this.commentFeignClient = commentFeignClient;
        this.circleFeignClient = circleFeignClient;
        this.sensitiveWordService = sensitiveWordService;
        this.aiReviewService = aiReviewService;
        this.postEsSyncService = postEsSyncService;
        this.postReviewService = postReviewService;
        this.postQueryService = postQueryService;
        this.aiReviewExecutor = aiReviewExecutor;
    }

    /**
     * 创建帖子
     */
    public Long createPost(Long userId, CreatePostRequest request) {
        if (request.getStatus() != null && request.getStatus() == PostStatus.PENDING) {
            validatePublishFields(request.getTitle(),
                    request.getCircleId(), request.getContent(), request.getImages(), request.getPostType(), userId);
            // 发布时检测敏感词，命中直接拒绝
            Set<String> hits = sensitiveWordService.check(request.getTitle(), request.getContent());
            if (!hits.isEmpty()) {
                throw new BusinessException(ErrorCode.CONTENT_SENSITIVE, "内容包含敏感词：" + String.join("、", hits));
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
        String postType = request.getPostType() != null ? request.getPostType() : PostType.NORMAL;
        po.setPostType(postType);
        po.setStatus(request.getStatus() != null ? request.getStatus() : PostStatus.DRAFT);
        po.setLikes(0);
        po.setComments(0);
        po.setViews(0);
        po.setCollections(0);
        postMapper.insert(po);
        if (po.getStatus() == PostStatus.PENDING) {
            // 异步调 AI 审核，审核结果在本服务内处理
            final Long postId = po.getId();
            final String postTitle = po.getTitle();
            final String postContent = po.getContent();
            final List<String> images = request.getImages();
            CompletableFuture.runAsync(() -> {
                try {
                    AiReviewResult result = aiReviewService.review(postId, postTitle, postContent, images);
                    postReviewService.handleReviewResult(postId, userId, postTitle, result);
                } catch (Exception e) {
                    postReviewService.handleReviewFailure(postId, e);
                }
            }, aiReviewExecutor);
        }
        log.info("创建帖子成功: postId={}, userId={}", po.getId(), userId);
        return po.getId();
    }

    /**
     * 更新帖子
     * <p>支持三种业务动作：保存草稿、草稿转发布/更新已发布、状态迁移。
     */
    public void updatePost(Long userId, UpdatePostRequest request) {
        PostPO po = postMapper.selectById(request.getIdAsLong());
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_OWNER);
        }

        // 记录原始状态，用于 CAS 更新防止并发状态覆盖
        final int originalStatus = po.getStatus();

        // 发布动作：仅校验请求体完整性，前端必须传完整发布数据
        boolean isPublishAction = request.getStatus() != null && request.getStatus() == PostStatus.PENDING;
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
                throw new BusinessException(ErrorCode.CONTENT_SENSITIVE, "内容包含敏感词：" + String.join("、", hits));
            }
        }

        applyContentUpdate(po, request);

        if (request.getStatus() != null) {
            if (!PostStatus.canTransition(po.getStatus(), request.getStatus())) {
                throw new BusinessException(ErrorCode.POST_STATUS_TRANSITION_INVALID,
                        "不允许从 " + PostStatus.label(po.getStatus()) + " 直接变更为 " + PostStatus.label(request.getStatus()));
            }
            po.setStatus(request.getStatus());
        }

        // CAS 更新：带原 status 条件，防止并发请求互相覆盖状态
        // 用最小化实体只更新业务字段，不携带 views/likes 等计数字段，避免覆盖并发原子递增
        PostPO update = new PostPO();
        applyContentUpdate(update, request);
        if (request.getStatus() != null) {
            update.setStatus(po.getStatus());
        }
        LambdaUpdateWrapper<PostPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PostPO::getId, po.getId())
               .eq(PostPO::getStatus, originalStatus);
        int rows = postMapper.update(update, wrapper);
        if (rows == 0) {
            log.warn("帖子状态迁移CAS失败(状态已被并发修改): postId={}, 期望原状态={}({})",
                    po.getId(), PostStatus.label(originalStatus), originalStatus);
            throw new BusinessException(ErrorCode.POST_STATUS_CONFLICT, "帖子状态已被修改，请刷新后重试");
        }
        if (request.getStatus() != null && originalStatus != request.getStatus()) {
            log.info("帖子状态迁移: postId={}, {}({})→{}({}), operatorId={}",
                    po.getId(), PostStatus.label(originalStatus), originalStatus,
                    PostStatus.label(request.getStatus()), request.getStatus(), userId);
        }
        postQueryService.evictDetailCache(po.getId());
        postEsSyncService.syncPostToEs(po);

        if (isPublishAction) {
            final Long postId = po.getId();
            final String postTitle = po.getTitle();
            final String postContent = po.getContent();
            final List<String> images = request.getImages();
            CompletableFuture.runAsync(() -> {
                try {
                    AiReviewResult result = aiReviewService.review(postId, postTitle, postContent, images);
                    postReviewService.handleReviewResult(postId, userId, postTitle, result);
                } catch (Exception e) {
                    postReviewService.handleReviewFailure(postId, e);
                }
            }, aiReviewExecutor);
        }

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
     */
    public void deletePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_OWNER);
        }
        softDelete(po);
        log.info("软删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 管理员删除帖子（逻辑删除，不校验作者归属）
     *
     * @param postId 帖子 ID
     */
    public void adminDeletePost(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        softDelete(po);
        log.info("管理员删除帖子: postId={}", postId);
    }

    /**
     * 圈子维度删帖（圈主/圈子管理员删除本圈帖子），校验帖子归属当前圈子
     *
     * @param circleId 圈子 ID
     * @param postId   帖子 ID
     */
    public void deletePostByCircle(Long circleId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!circleId.equals(po.getCircleId())) {
            throw new BusinessException(ErrorCode.POST_NOT_IN_CIRCLE);
        }
        softDelete(po);
        log.info("圈子管理员删除帖子: circleId={}, postId={}", circleId, postId);
    }

    /**
     * 软删帖子公共逻辑：置状态为 DELETED → 更新 → 清缓存 → 同步 ES → 发计数事件
     * <p>调用方负责 select、权限/归属校验与日志输出。
     * <p>仅原状态为 APPROVED 时发 DELETE 计数事件（圈子 post_count -1）。
     *
     * @param po 已加载并校验通过的帖子实体
     */
    private void softDelete(PostPO po) {
        boolean wasApproved = po.getStatus() == PostStatus.APPROVED;
        po.setStatus(PostStatus.DELETED);
        postMapper.updateById(po);
        postQueryService.evictDetailCache(po.getId());
        postEsSyncService.syncPostToEs(po);
        if (wasApproved) {
            postEsSyncService.publishCountEvent(po, PostCountConstants.ACTION_DELETE);
        }
    }

    /**
     * 彻底删除帖子（物理删除 + 级联清理关联数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public void hardDeletePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_OWNER);
        }
        if (po.getStatus() != PostStatus.DELETED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅回收站中的帖子可彻底删除");
        }

        // 本地事务：仅删除本服务数据，保证原子性
        // 1. 删除帖子点赞
        postLikeMapper.delete(
                new LambdaQueryWrapper<PostLikePO>()
                    .eq(PostLikePO::getPostId, postId));

        // 2. 删除帖子收藏
        postCollectMapper.delete(
                new LambdaQueryWrapper<PostCollectPO>()
                    .eq(PostCollectPO::getPostId, postId));

        // 3. 删除帖子主表
        postMapper.deleteById(postId);
        postQueryService.evictDetailCache(postId);

        // ES 同步删除 + 跨服务评论清理均放到事务提交后执行：
        // 避免事务回滚后 ES 已删/评论已清导致的不可恢复不一致，同时缩短事务持有 DB 连接的时间
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                postEsSyncService.syncPostToEsDelete(postId);
                try {
                    Result<Void> commentResult = commentFeignClient.deleteByPostId(postId);
                    if (commentResult == null || !commentResult.isSuccess()) {
                        log.warn("删除帖子关联评论失败: postId={}, result={}", postId, commentResult);
                    }
                } catch (Exception e) {
                    log.error("删除帖子关联评论异常: postId={}", postId, e);
                }
            }
        });
        log.info("彻底删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 置顶帖子
     */
    public void pinPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_OWNER);
        }
        if (po.getStatus() != PostStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已发布帖子可置顶");
        }
        // 原子置顶：SQL 层校验已置顶数 < 3，避免 TOCTOU 竞态
        int rows = postMapper.pinPost(userId, postId);
        if (rows == 0) {
            if (po.getPinned() != null && po.getPinned() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子已置顶");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最多置顶 3 条帖子");
        }
        log.info("置顶帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 取消置顶帖子
     */
    public void unpinPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_OWNER);
        }
        postMapper.unpinPost(userId, postId);
        log.info("取消置顶帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 批量操作帖子（发布/删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchOperate(Long userId, List<Long> postIds, String action) {
        if (postIds == null || postIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要操作的帖子");
        }
        if ("publish".equals(action)) {
            // 批量查询替代循环 selectById，避免 N+1
            List<PostPO> posts = postMapper.selectBatchIds(postIds);
            List<Long> validPostIds = new ArrayList<>();
            for (PostPO po : posts) {
                if (!po.getUserId().equals(userId)) continue;
                if (po.getStatus() != PostStatus.DRAFT) continue;
                // 草稿转发布需校验必填字段
                if (po.getTitle() == null || po.getTitle().isBlank()
                        || po.getContent() == null || po.getContent().isBlank()
                        || po.getImages() == null || po.getImages().isBlank()) {
                    log.warn("草稿缺少必填字段，跳过: postId={}", po.getId());
                    continue;
                }
                validPostIds.add(po.getId());
                // 异步 AI 审核（事务提交后派发，避免读到旧状态）
                final Long pid = po.getId();
                final String title = po.getTitle();
                final String content = po.getContent();
                List<String> imgs = po.getImages() != null && !po.getImages().isBlank()
                        ? Arrays.asList(po.getImages().split(",")) : Collections.emptyList();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                AiReviewResult result = aiReviewService.review(pid, title, content, imgs);
                                postReviewService.handleReviewResult(pid, userId, title, result);
                            } catch (Exception e) {
                                postReviewService.handleReviewFailure(pid, e);
                            }
                        }, aiReviewExecutor);
                    }
                });
            }
            if (!validPostIds.isEmpty()) {
                // 仅草稿(DRAFT)帖子可转发布，防止已发布帖被打回 PENDING
                int rows = postMapper.batchUpdateStatus(userId, validPostIds, PostStatus.PENDING, PostStatus.DRAFT);
                log.info("批量发布状态迁移: 选中={}, 实际迁移(DRAFT→PENDING)={}, userId={}",
                        validPostIds.size(), rows, userId);
            }
        } else if ("delete".equals(action)) {
            // 先查出 APPROVED 状态的帖子，批量删除后发计数事件（post_count -1）
            List<PostPO> approvedPosts = postMapper.selectList(
                    new LambdaQueryWrapper<PostPO>()
                            .eq(PostPO::getUserId, userId)
                            .in(PostPO::getId, postIds)
                            .eq(PostPO::getStatus, PostStatus.APPROVED));
            postMapper.batchUpdateStatus(userId, postIds, PostStatus.DELETED, null);
            for (PostPO po : approvedPosts) {
                postEsSyncService.publishCountEvent(po, PostCountConstants.ACTION_DELETE);
            }
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的操作类型: " + action);
        }
        log.info("批量操作完成: action={}, count={}, userId={}", action, postIds.size(), userId);
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
        if (!PostType.ARTICLE.equals(postType) && (images == null || images.isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "图文帖发布时至少需要一张图片");
        }
        boolean isArticle = PostType.ARTICLE.equals(postType);
        int titleLen = title.length();
        int maxTitleLen = isArticle ? 50 : 30;
        if (titleLen > maxTitleLen) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    isArticle ? "长文标题最长50字" : "图文标题最长30字");
        }
        int contentLen = content.length();
        int maxContentLen = isArticle ? 50000 : 10000;
        if (contentLen > maxContentLen) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    isArticle ? "长文正文最长50000字" : "图文正文最长10000字");
        }
        if (isArticle && contentLen < 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "长文正文至少100字");
        }
        Result<PublishableResult> publishableResult = circleFeignClient.checkPublishable(circleId, userId);
        // 服务降级：圈子服务不可用，不假成功避免越权发布
        if (publishableResult == null || !publishableResult.isSuccess()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "圈子服务暂不可用，请稍后重试");
        }
        PublishableResult data = publishableResult.getData();
        if (data == null || !data.isPublishable()) {
            if (data != null && !data.isExists()) {
                throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
            }
            if (data != null && !data.isEnabled()) {
                throw new BusinessException(ErrorCode.CIRCLE_DISABLED, "该圈子已停用，暂不可发布");
            }
            throw new BusinessException(ErrorCode.NOT_CIRCLE_MEMBER, "请先加入该圈子再发布");
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
}
