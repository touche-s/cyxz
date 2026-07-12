package com.cyxz.comment.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentService;
import com.cyxz.comment.vo.CommentVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发表评论
     * <p>创建一条评论记录，支持顶级评论和回复（通过 parentId 区分）。
     * 点赞数初始化为 0，状态默认正常。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建评论请求（含帖子 ID、内容、父评论 ID、被回复用户 ID）
     * @return 新创建的评论 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long userId, CreateCommentRequest request) {
        CommentPO po = new CommentPO();
        po.setPostId(request.getPostId());
        po.setUserId(userId);
        po.setContent(request.getContent());
        po.setParentId(request.getParentId());
        po.setReplyToUserId(request.getReplyToUserId());
        po.setLikes(0);
        po.setStatus(1);
        commentMapper.insert(po);
        log.info("发表评论成功: commentId={}, postId={}, userId={}", po.getId(), po.getPostId(), userId);
        return po.getId();
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
     * 分页查询帖子的顶级评论列表（两级分页）
     * <p>Step 1: SQL 分页查顶级评论（parent_id IS NULL）
     * <p>Step 2: 批量查这些顶级评论的子回复（只带第一页 3 条）
     * <p>Step 3: 注入 totalReplies / hasMoreReplies 供前端"展开更多回复"
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 分页结果（仅顶级评论计入分页）
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

        // Step 2: 批量查这些顶级评论的子回复
        List<Long> parentIds = topComments.stream()
                .map(CommentPO::getId).collect(Collectors.toList());
        LambdaQueryWrapper<CommentPO> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.in(CommentPO::getParentId, parentIds)
                .eq(CommentPO::getStatus, 1)
                .orderByAsc(CommentPO::getCreateTime);
        List<CommentPO> allChildren = commentMapper.selectList(childWrapper);

        // 按 parentId 分组，并统计每组的子回复总数
        Map<Long, List<CommentPO>> childrenByParent = allChildren.stream()
                .collect(Collectors.groupingBy(CommentPO::getParentId, LinkedHashMap::new, Collectors.toList()));

        // 收集所有 userId
        Set<Long> userIds = new HashSet<>();
        for (CommentPO c : topComments) userIds.add(c.getUserId());
        for (CommentPO c : allChildren) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }

        // Step 3: 批量查用户信息 + 点赞状态
        Map<Long, UserProfileVO> userMap = getUserMap(userIds);
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId);

        // Step 4: 组装结果 —— 顶级评论带第一页子评论
        int childPageSize = 3; // 子回复第一页大小
        List<CommentVO> result = new ArrayList<>();
        for (CommentPO top : topComments) {
            CommentVO topVO = toVO(top, userMap, likedCommentIds);
            List<CommentPO> children = childrenByParent.getOrDefault(top.getId(), Collections.emptyList());
            int totalReplies = children.size();
            // 只取第一页子评论
            List<CommentPO> firstPage = children.subList(0, Math.min(childPageSize, totalReplies));
            List<CommentVO> childVOs = convertToVOList(firstPage, userMap, likedCommentIds);
            topVO.setChildren(childVOs);
            topVO.setTotalReplies(totalReplies);
            topVO.setHasMoreReplies(totalReplies > childPageSize);
            result.add(topVO);
        }

        return PageResult.of(result, (int) topPage.getTotal(), page, size);
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
     * <p>使用 Redis Set 记录用户的点赞评论 ID，已点赞则取消（likes - 1），
     * 未点赞则点赞（likes + 1），通过 SQL 原子更新避免并发问题。
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

        String key = CacheKeyConstants.USER_LIKED_COMMENTS + userId;
        Boolean exists = stringRedisTemplate.opsForSet().isMember(key, commentId.toString());

        LambdaUpdateWrapper<CommentPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommentPO::getId, commentId);

        if (Boolean.TRUE.equals(exists)) {
            // 已点赞 → 取消点赞（GREATEST 防止并发导致负数）
            wrapper.setSql("likes = GREATEST(likes - 1, 0)");
            commentMapper.update(null, wrapper);
            stringRedisTemplate.opsForSet().remove(key, commentId.toString());
            log.info("取消点赞评论: commentId={}, userId={}", commentId, userId);
        } else {
            // 未点赞 → 点赞
            wrapper.setSql("likes = likes + 1");
            commentMapper.update(null, wrapper);
            stringRedisTemplate.opsForSet().add(key, commentId.toString());
            log.info("点赞评论: commentId={}, userId={}", commentId, userId);
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
     */
    private Set<Long> getLikedCommentIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String key = CacheKeyConstants.USER_LIKED_COMMENTS + userId;
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (CollUtil.isEmpty(members)) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Long::valueOf)
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
}
