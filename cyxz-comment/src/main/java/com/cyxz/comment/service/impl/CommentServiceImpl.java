package com.cyxz.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.cyxz.post.feign.PostFeignClient;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long userId, CreateCommentRequest request) {
        CommentPO po = new CommentPO();
        po.setPostId(request.getPostId());
        po.setUserId(userId);
        po.setContent(request.getContent());
        po.setParentId(request.getParentId());
        po.setReplyToUserId(request.getReplyToUserId());
        po.setLikes(0);
        po.setStatus(1);

        try {
            Result<Long> result = postFeignClient.getPostAuthor(request.getPostId());
            if (result != null && result.getData() != null) {
                po.setPostAuthorId(result.getData());
            }
        } catch (Exception e) {
            log.warn("获取帖子作者失败: postId={}", request.getPostId(), e);
        }

        commentMapper.insert(po);
        log.info("发表评论成功: commentId={}, postId={}, userId={}", po.getId(), po.getPostId(), userId);

        // 组装完整 VO 返回给前端，前端可直接插入列表展示
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(userId);
        if (po.getReplyToUserId() != null) {
            userIds.add(po.getReplyToUserId());
        }
        Map<Long, UserProfileVO> userMap = getUserMap(userIds);
        return toVO(po, userMap, Collections.emptySet()); // 刚创建，当前用户不可能已点赞
    }

    /**
     * 删除评论（逻辑删除）
     * <p>将评论状态设为 0（已删除），不做物理删除。
     * 校验评论归属权，非作者本人无权删除。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        po.setStatus(0);
        commentMapper.updateById(po);
        log.info("删除评论成功: commentId={}, userId={}", commentId, userId);
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
                .eq(CommentPO::getStatus, 1)
                .isNull(CommentPO::getParentId)
                .orderByAsc(CommentPO::getCreateTime);
        Page<CommentPO> topPage = commentMapper.selectPage(
                new Page<>(page, size), topWrapper);

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
        Map<Long, UserProfileVO> userMap = getUserMap(userIds);
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId);

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
        countWrapper.eq(CommentPO::getPostId, postId).eq(CommentPO::getStatus, 1);
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
                .eq(CommentPO::getStatus, 1)
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
                .eq(CommentPO::getStatus, 1)
                .orderByAsc(CommentPO::getCreateTime);
        Page<CommentPO> replyPage = commentMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<CommentPO> replies = replyPage.getRecords();
        if (replies.isEmpty()) {
            return PageResult.empty(page, size);
        }

        // 收集 userId
        Set<Long> userIds = new HashSet<>();
        for (CommentPO c : replies) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }

        Map<Long, UserProfileVO> userMap = getUserMap(userIds);
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId);

        List<CommentVO> vos = convertToVOList(replies, userMap, likedCommentIds);
        return PageResult.of(vos, (int) replyPage.getTotal(), page, size);
    }

    /**
     * 点赞 / 取消点赞评论
     * <p>使用 comment_like 表存储用户点赞关系（逻辑状态型）。
     * 不存在则插入 status=1，已存在则切换 status，同时原子更新 comment.likes。
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     * @return 操作后的点赞数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleLike(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 查询是否已存在点赞关系
        LambdaQueryWrapper<CommentLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLikePO::getUserId, userId)
                .eq(CommentLikePO::getCommentId, commentId);
        CommentLikePO likePO = commentLikeMapper.selectOne(wrapper);

        LambdaUpdateWrapper<CommentPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CommentPO::getId, commentId);

        if (likePO == null) {
            // 不存在 → 插入 status=1
            CommentLikePO newLike = new CommentLikePO();
            newLike.setCommentId(commentId);
            newLike.setUserId(userId);
            newLike.setStatus(1);
            commentLikeMapper.insert(newLike);
            updateWrapper.setSql("likes = likes + 1");
            commentMapper.update(null, updateWrapper);
            log.info("点赞评论: commentId={}, userId={}", commentId, userId);
        } else if (likePO.getStatus() == 0) {
            // 已取消 → 恢复点赞
            likePO.setStatus(1);
            commentLikeMapper.updateById(likePO);
            updateWrapper.setSql("likes = likes + 1");
            commentMapper.update(null, updateWrapper);
            log.info("点赞评论(恢复): commentId={}, userId={}", commentId, userId);
        } else {
            // 已点赞 → 取消点赞
            likePO.setStatus(0);
            commentLikeMapper.updateById(likePO);
            updateWrapper.setSql("likes = GREATEST(likes - 1, 0)");
            commentMapper.update(null, updateWrapper);
            log.info("取消点赞评论: commentId={}, userId={}", commentId, userId);
        }

        // 查询最新点赞数
        CommentPO updated = commentMapper.selectById(commentId);
        return updated.getLikes();
    }

    /**
     * 批量查询用户信息
     */
    private Map<Long, UserProfileVO> getUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<Map<Long, UserProfileVO>> result = userFeignClient.batchGetByIds(new ArrayList<>(userIds));
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("批量查询用户信息失败", e);
        }
        return Collections.emptyMap();
    }

    /**
     * 获取当前用户已点赞的评论 ID 集合
     * <p>从 comment_like 表批量查询当前用户 status=1 的点赞记录。
     *
     * @param userId 当前登录用户 ID（可为 null）
     * @return 已点赞评论 ID 集合
     */
    private Set<Long> getLikedCommentIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<CommentLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLikePO::getUserId, userId)
                .eq(CommentLikePO::getStatus, 1)
                .select(CommentLikePO::getCommentId);
        List<CommentLikePO> list = commentLikeMapper.selectList(wrapper);
        return list.stream()
                .map(CommentLikePO::getCommentId)
                .collect(Collectors.toSet());
    }

    /**
     * 批量转换 PO 列表为 VO 列表
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

    @Override
    public PageResult<CommentVO> listReceivedComments(Long userId, int page, int size) {
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPO::getPostAuthorId, userId)
                .eq(CommentPO::getStatus, 1)
                .orderByDesc(CommentPO::getCreateTime);

        Page<CommentPO> pageResult = commentMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<CommentPO> comments = pageResult.getRecords();
        if (comments.isEmpty()) {
            return PageResult.empty(page, size);
        }

        Set<Long> userIds = comments.stream()
                .map(CommentPO::getUserId)
                .collect(Collectors.toSet());
        for (CommentPO comment : comments) {
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }
        Map<Long, UserProfileVO> userMap = getUserMap(userIds);

        List<CommentVO> voList = convertToVOList(comments, userMap, Collections.emptySet());

        return PageResult.of(voList, pageResult.getTotal(), page, size);
    }
}
