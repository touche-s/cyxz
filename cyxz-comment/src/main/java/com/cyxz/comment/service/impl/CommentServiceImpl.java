package com.cyxz.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.entity.CommentLikePO;
import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentLikeMapper;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentService;
import com.cyxz.comment.vo.CommentVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.utils.FeignResults;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.utils.NotificationPublisher;
import com.cyxz.post.feign.PostFeignClient;
import com.cyxz.post.vo.PostInfoVO;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;
    private final CircleFeignClient circleFeignClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * 发表评论
     * <p>创建一条评论记录，支持顶级评论和回复（通过 parentId 区分）。
     * 点赞数初始化为 0，状态默认正常。
     * <p>插入成功后查询当前用户和被回复用户的资料，转换为完整 VO 返回给前端，
     * 前端可直接插入评论列表展示，避免刷新。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建评论请求（含帖子 ID、内容、父评论 ID、被回复用户 ID）
     * @return 新创建的评论视图对象（含完整用户信息）
     */
    @Override
    public CommentVO createComment(Long userId, CreateCommentRequest request) {
        CommentPO po = new CommentPO();
        po.setPostId(request.getPostIdAsLong());
        po.setUserId(userId);
        po.setContent(request.getContent());
        po.setParentId(request.getParentIdAsLong());
        po.setReplyToUserId(request.getReplyToUserIdAsLong());
        po.setLikes(0);
        po.setStatus(CommonStatus.ACTIVE);

        // 校验 parentId：非空时校验父评论存在且属于同一帖子（只读 DB，无需事务）
        if (po.getParentId() != null) {
            CommentPO parent = commentMapper.selectById(po.getParentId());
            if (parent == null || parent.getStatus() == CommonStatus.DELETED) {
                throw new BusinessException(ErrorCode.COMMENT_PARENT_NOT_FOUND, "父评论不存在");
            }
            if (!parent.getPostId().equals(po.getPostId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "父评论不属于该帖子");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.COMMENT_NO_MULTI_LEVEL, "不支持多级回复，请直接回复顶级评论");
            }
        }

        // Feign：拿帖子作者 + 圈子 ID（事务外，不占 DB 连接）
        Result<PostInfoVO> postInfoResult = postFeignClient.getPostInfo(request.getPostIdAsLong());
        if (postInfoResult == null || !postInfoResult.isSuccess()) {
            log.warn("获取帖子信息失败(服务降级): postId={}, result={}", request.getPostId(), postInfoResult);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "帖子服务暂不可用，请稍后重试");
        }
        PostInfoVO postInfo = postInfoResult.getData();
        if (postInfo == null || postInfo.getUserId() == null) {
            log.warn("获取帖子信息失败: postId={}, result={}", request.getPostId(), postInfoResult);
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        po.setPostAuthorId(postInfo.getUserId());

        // Feign：校验圈子成员（事务外）
        if (postInfo.getCircleId() != null) {
            Long circleId = postInfo.getCircleId();
            Result<PublishableResult> publishableResult = circleFeignClient.checkPublishable(circleId, userId);
            // 服务降级：圈子服务不可用，不假成功避免越权评论
            if (publishableResult == null || !publishableResult.isSuccess()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "圈子服务暂不可用，请稍后重试");
            }
            PublishableResult circleData = publishableResult.getData();
            if (circleData == null || !circleData.isJoined()) {
                throw new BusinessException(ErrorCode.NOT_CIRCLE_MEMBER, "请先加入该圈子再评论");
            }
        }

        // DB 写 + Redis + 注册 afterCommit 发 MQ（编程式事务）
        transactionTemplate.executeWithoutResult(status -> {
            commentMapper.insert(po);

            stringRedisTemplate.opsForHash()
                    .increment(CacheKeyConstants.POST_COMMENT_DELTA, po.getPostId().toString(), 1);

            List<NotificationEvent> events = new ArrayList<>();
            if (!userId.equals(po.getPostAuthorId())) {
                events.add(NotificationEvent.builder()
                        .receiverId(po.getPostAuthorId())
                        .senderId(userId)
                        .type(NotificationType.POST_COMMENTED.name())
                        .title("有人评论了你的帖子")
                        .targetType("comment")
                        .targetId(po.getId())
                        .relatedId(po.getPostId())
                        .content(request.getContent())
                        .createTime(System.currentTimeMillis())
                        .build());
            }
            if (po.getReplyToUserId() != null && !userId.equals(po.getReplyToUserId())) {
                events.add(NotificationEvent.builder()
                        .receiverId(po.getReplyToUserId())
                        .senderId(userId)
                        .type(NotificationType.COMMENT_REPLIED.name())
                        .title("有人回复了你的评论")
                        .targetType("comment")
                        .targetId(po.getId())
                        .relatedId(po.getPostId())
                        .content(request.getContent())
                        .createTime(System.currentTimeMillis())
                        .build());
            }

            if (!events.isEmpty()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        events.forEach(e -> NotificationPublisher.publish(rabbitTemplate, e));
                    }
                });
            }
        });

        log.info("发表评论成功: commentId={}, postId={}, userId={}", po.getId(), po.getPostId(), userId);

        // Feign：组装 VO（事务外）
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(userId);
        if (po.getReplyToUserId() != null) {
            userIds.add(po.getReplyToUserId());
        }
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);
        return toVO(po, userMap, Collections.emptySet());
    }

    /**
     * 删除评论（逻辑删除）
     * <p>将评论状态设为 0（已删除），不做物理删除。
     * 权限：评论作者本人 或 帖子作者可删除。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == CommonStatus.DELETED) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        boolean isCommentAuthor = po.getUserId().equals(userId);
        boolean isPostAuthor = po.getPostAuthorId() != null && po.getPostAuthorId().equals(userId);
        if (!isCommentAuthor && !isPostAuthor) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_OWNER);
        }
        po.setStatus(CommonStatus.DELETED);
        commentMapper.updateById(po);
        // 级联逻辑删除子回复
        int replyCount = commentMapper.cascadeDeleteReplies(commentId);
        // Redis 增量：帖子评论数 -(1 + 子回复数)
        stringRedisTemplate.opsForHash()
                .increment(CacheKeyConstants.POST_COMMENT_DELTA, po.getPostId().toString(), -(1 + replyCount));
        log.info("删除评论成功: commentId={}, userId={}, 级联删除子回复={}", commentId, userId, replyCount);
    }

    /**
     * 分页查询帖子的顶级评论列表（按需加载子回复）
     * <p>仅返回顶级评论自身，不预加载子回复。
     * <p>子回复总数通过 COUNT 统计写入 totalReplies，前端按需调用 /comment/replies 加载。
     * <p>注意：分页只针对顶级评论，但返回的 total 是该帖子全部评论数（顶级 + 子回复），
     * 用于详情页展示"评论 (N)"。
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 分页结果（仅顶级评论计入分页，children 为空，total 为全部评论数）
     */
    @Override
    public PageResult<CommentVO> listComments(Long postId, int page, int size, Long currentUserId) {
        // Step 1: SQL 分页查顶级评论
        LambdaQueryWrapper<CommentPO> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.eq(CommentPO::getPostId, postId)
                .eq(CommentPO::getStatus, CommonStatus.ACTIVE)
                .isNull(CommentPO::getParentId)
                .orderByAsc(CommentPO::getCreateTime);
        Page<CommentPO> topPage = commentMapper.selectPage(
                PageConstants.pageOf(page, size), topWrapper);

        List<CommentPO> topComments = topPage.getRecords();
        if (topComments.isEmpty()) {
            return PageResult.empty(page, size);
        }

        // Step 2: 统计每条顶级评论的子回复总数（轻量 COUNT，只查 parentId）
        List<Long> parentIds = topComments.stream()
                .map(CommentPO::getId).collect(Collectors.toList());
        Map<Long, Integer> replyCounts = countRepliesByParents(parentIds);

        // Step 3: 收集用户 ID（仅顶级评论的作者）
        Set<Long> userIds = topComments.stream()
                .map(CommentPO::getUserId)
                .collect(Collectors.toSet());

        // Step 4: 批量查用户信息 + 点赞状态
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);
        Set<Long> topCommentIds = topComments.stream()
                .map(CommentPO::getId)
                .collect(Collectors.toSet());
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId, topCommentIds);

        // Step 5: 组装结果（children 为空，子回复全由 /comment/replies 按需加载）
        List<CommentVO> result = topComments.stream().map(top -> {
            CommentVO vo = toVO(top, userMap, likedCommentIds);
            int totalReplies = replyCounts.getOrDefault(top.getId(), 0);
            vo.setChildren(Collections.emptyList());
            vo.setTotalReplies(totalReplies);
            vo.setHasMoreReplies(totalReplies > 0);
            return vo;
        }).collect(Collectors.toList());

        // Step 6: total 返回该帖子全部评论数（顶级 + 子回复），用于详情页展示
        LambdaQueryWrapper<CommentPO> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(CommentPO::getPostId, postId).eq(CommentPO::getStatus, CommonStatus.ACTIVE);
        Long totalComments = commentMapper.selectCount(countWrapper);

        return PageResult.of(result, totalComments, page, size);
    }

    /**
     * 统计一批父评论的子回复数量
     */
    private Map<Long, Integer> countRepliesByParents(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CommentPO::getParentId, parentIds)
                .eq(CommentPO::getStatus, CommonStatus.ACTIVE)
                .select(CommentPO::getParentId, CommentPO::getId);
        List<CommentPO> replies = commentMapper.selectList(wrapper);
        return replies.stream()
                .collect(Collectors.groupingBy(
                        CommentPO::getParentId,
                        Collectors.summingInt(c -> 1)));
    }

    /**
     * 分页查询某条评论的子回复
     * <p>前端点击"展开更多回复"时调用。
     *
     * @param parentId      父评论 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 分页结果
     */
    @Override
    public PageResult<CommentVO> listReplies(Long parentId, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPO::getParentId, parentId)
                .eq(CommentPO::getStatus, CommonStatus.ACTIVE)
                .orderByAsc(CommentPO::getCreateTime);
        Page<CommentPO> replyPage = commentMapper.selectPage(
                PageConstants.pageOf(page, size), wrapper);

        List<CommentPO> replies = replyPage.getRecords();
        if (replies.isEmpty()) {
            return PageResult.empty(page, size);
        }

        // 收集 userId
        Set<Long> userIds = collectUserIds(replies);

        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);
        Set<Long> replyCommentIds = replies.stream()
                .map(CommentPO::getId)
                .collect(Collectors.toSet());
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId, replyCommentIds);

        List<CommentVO> vos = convertToVOList(replies, userMap, likedCommentIds);
        return PageResult.of(vos, (int) replyPage.getTotal(), page, size);
    }

    /**
     * 获取当前用户在指定评论集合中已点赞的评论 ID
     * <p>从 comment_like 表按 commentId IN (...) 查询，避免拉取用户全量点赞记录。
     *
     * @param userId      当前登录用户 ID（可为 null）
     * @param commentIds  当前页评论 ID 集合
     * @return 已点赞评论 ID 集合
     */
    private Set<Long> getLikedCommentIds(Long userId, Set<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<CommentLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLikePO::getUserId, userId)
                .eq(CommentLikePO::getStatus, CommonStatus.ACTIVE)
                .in(CommentLikePO::getCommentId, commentIds)
                .select(CommentLikePO::getCommentId);
        List<CommentLikePO> list = commentLikeMapper.selectList(wrapper);
        return list.stream()
                .map(CommentLikePO::getCommentId)
                .collect(Collectors.toSet());
    }

    /**
     * 收集评论列表中涉及的用户 ID
     * <p>遍历评论列表，提取评论作者与被回复者 ID，null 安全。
     *
     * @param comments 评论实体列表
     * @return 去重后的用户 ID 集合
     */
    private Set<Long> collectUserIds(List<CommentPO> comments) {
        Set<Long> userIds = new HashSet<>();
        for (CommentPO c : comments) {
            if (c.getUserId() != null) userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }
        return userIds;
    }

    /**
     * 批量填充评论 VO 列表（含帖子标题）
     * <p>统一查询用户信息、点赞状态、帖子标题，并将实体列表转换为 VO 列表。
     * 供"收到的评论"与"评论管理"两个分页接口共用。
     *
     * @param comments      评论实体列表
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 评论 VO 列表
     */
    private List<CommentVO> fillCommentVOListWithPost(List<CommentPO> comments, Long currentUserId) {
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, collectUserIds(comments));
        Set<Long> commentIds = comments.stream()
                .map(CommentPO::getId)
                .collect(Collectors.toSet());
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId, commentIds);
        Map<Long, String> postTitleMap = getPostTitles(
                comments.stream().map(CommentPO::getPostId).collect(Collectors.toSet()));
        return comments.stream()
                .map(po -> {
                    CommentVO vo = toVO(po, userMap, likedCommentIds);
                    vo.setPostTitle(postTitleMap.getOrDefault(po.getPostId(), ""));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量转换 PO 列表为 VO 列表
     * <p>统一将评论实体列表转换为视图对象列表，用于评论/回复分页结果填充。
     *
     * @param pos             评论实体列表
     * @param userMap         用户 ID → UserProfileVO 映射
     * @param likedCommentIds 当前用户已点赞的评论 ID 集合
     * @return 评论视图对象列表
     */
    private List<CommentVO> convertToVOList(List<CommentPO> pos, Map<Long, UserProfileVO> userMap,
                                             Set<Long> likedCommentIds) {
        return pos.stream()
                .map(po -> toVO(po, userMap, likedCommentIds))
                .collect(Collectors.toList());
    }

    /**
     * 转换为视图对象
     */
    private CommentVO toVO(CommentPO po, Map<Long, UserProfileVO> userMap, Set<Long> likedCommentIds) {
        CommentVO vo = new CommentVO();
        vo.setId(po.getId());
        vo.setPostId(po.getPostId());
        vo.setUserId(po.getUserId());
        UserProfileVO author = userMap.get(po.getUserId());
        vo.setUserName(author != null ? author.getNickname() : "未知用户");
        vo.setUserAvatar(author != null ? author.getAvatar() : "");
        vo.setContent(po.getContent());
        vo.setParentId(po.getParentId());
        vo.setReplyToUserId(po.getReplyToUserId());
        if (po.getReplyToUserId() != null) {
            UserProfileVO replyTo = userMap.get(po.getReplyToUserId());
            vo.setReplyToUserName(replyTo != null ? replyTo.getNickname() : "未知用户");
        }
        vo.setLikes(po.getLikes());
        vo.setLiked(likedCommentIds.contains(po.getId()));
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 查询用户收到的评论列表（评论我的帖子 + 回复我的评论）
     * <p>查询条件：状态正常 且 评论作者不是自己 且（帖子作者是自己 或 被回复者是自己）。
     * 排除自己评论自己的记录，按创建时间倒序分页。
     *
     * @param userId        被查看的用户 ID（用于筛选收到的评论）
     * @param currentUserId 当前登录用户 ID（用于查点赞状态）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @return 分页结果（含帖子标题、回复目标用户昵称）
     */
    @Override
    public PageResult<CommentVO> listReceivedComments(Long userId, Long currentUserId, int page, int size) {
        // 查"评论我的帖子"或"回复我的评论"，排除自己
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPO::getStatus, CommonStatus.ACTIVE)
                .ne(CommentPO::getUserId, userId)
                .and(w -> w
                        .eq(CommentPO::getPostAuthorId, userId)
                        .or()
                        .eq(CommentPO::getReplyToUserId, userId)
                )
                .orderByDesc(CommentPO::getCreateTime);

        Page<CommentPO> pageResult = commentMapper.selectPage(
                PageConstants.pageOf(page, size), wrapper);

        List<CommentPO> comments = pageResult.getRecords();
        if (comments.isEmpty()) {
            return PageResult.empty(page, size);
        }

        // 收集用户 ID
        Set<Long> userIds = new HashSet<>();
        for (CommentPO comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);

        // 查当前用户在本页评论中已点赞的评论 ID
        Set<Long> commentIds = comments.stream()
                .map(CommentPO::getId)
                .collect(Collectors.toSet());
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId, commentIds);

        // 批量查帖子标题
        Set<Long> postIds = comments.stream()
                .map(CommentPO::getPostId)
                .collect(Collectors.toSet());
        Map<Long, String> postTitleMap = getPostTitles(postIds);

        List<CommentVO> voList = comments.stream()
                .map(po -> {
                    CommentVO vo = toVO(po, userMap, likedCommentIds);
                    vo.setPostTitle(postTitleMap.getOrDefault(po.getPostId(), ""));
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(voList, pageResult.getTotal(), page, size);
    }

    /**
     * 批量查询帖子标题
     * <p>通过 Feign 调用 post 服务批量获取帖子标题，用于填充收到的评论列表中的帖子信息。
     * 单个帖子查询失败不影响整体结果，仅记录警告日志。
     *
     * @param postIds 帖子 ID 集合
     * @return 帖子 ID 到标题的映射 Map
     */
    private Map<Long, String> getPostTitles(Set<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return FeignResults.unwrapOrEmpty(postFeignClient.batchGetPostInfo(postIds)).stream()
                .collect(Collectors.toMap(PostInfoVO::getPostId, PostInfoVO::getTitle, (a, b) -> a));
    }

    /**
     * 评论管理：查询当前用户自己帖子下的评论
     * <p>按 postAuthorId = currentUserId 筛选，传 postId 时再补充 postId 条件。
     * 含自己评论自己的也会被查到（postAuthorId = userId 且 userId = userId），
     * 但实际场景少且管理页应展示全貌，予以保留。
     *
     * @param currentUserId 当前登录用户 ID（作为帖子作者筛选）
     * @param postId        帖子 ID（可选，null 表示查所有帖子）
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @return 分页结果（含帖子标题、回复目标用户昵称）
     */
    @Override
    public PageResult<CommentVO> listManagedComments(Long currentUserId, Long postId, int page, int size, boolean sortAsc) {
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPO::getStatus, CommonStatus.ACTIVE)
                .eq(CommentPO::getPostAuthorId, currentUserId);
        if (postId != null) {
            wrapper.eq(CommentPO::getPostId, postId);
        }
        if (sortAsc) {
            wrapper.orderByAsc(CommentPO::getCreateTime);
        } else {
            wrapper.orderByDesc(CommentPO::getCreateTime);
        }

        Page<CommentPO> pageResult = commentMapper.selectPage(
                PageConstants.pageOf(page, size), wrapper);

        List<CommentPO> comments = pageResult.getRecords();
        if (comments.isEmpty()) {
            return PageResult.empty(page, size);
        }

        return PageResult.of(fillCommentVOListWithPost(comments, currentUserId), pageResult.getTotal(), page, size);
    }

    /**
     * 删除指定帖子下的所有评论及评论点赞（物理删除）
     * <p>先查帖子下所有评论 ID，再批量删 comment_like 和 comment，用于帖子彻底删除时的级联清理。
     *
     * @param postId 帖子 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentsByPostId(Long postId) {
        List<Long> commentIds = commentMapper.selectList(
                new LambdaQueryWrapper<CommentPO>()
                    .eq(CommentPO::getPostId, postId)
                    .select(CommentPO::getId))
                .stream()
                .map(CommentPO::getId)
                .collect(Collectors.toList());

        if (!commentIds.isEmpty()) {
            commentLikeMapper.delete(
                    new LambdaQueryWrapper<CommentLikePO>()
                        .in(CommentLikePO::getCommentId, commentIds));
            log.info("删除帖子关联评论点赞: postId={}, commentCount={}", postId, commentIds.size());
        }

        commentMapper.delete(
                new LambdaQueryWrapper<CommentPO>()
                    .eq(CommentPO::getPostId, postId));
        log.info("删除帖子关联评论: postId={}, count={}", postId, commentIds.size());
    }

    /**
     * 统计今日某用户帖子收到的新评论数
     */
    @Override
    public int countTodayComments(Long postAuthorId) {
        return commentMapper.countTodayComments(postAuthorId);
    }
}
