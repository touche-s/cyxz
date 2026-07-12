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
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.base.Result;
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
     * 分页查询帖子的评论列表
     * <p>查询指定帖子的所有正常评论，构建父子嵌套关系，
     * 仅对顶级评论分页返回，子评论通过 children 字段嵌套。
     * 游客也可查看，currentUserId 为 null 时不标记点赞状态。
     *
     * @param postId        帖子 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 评论视图列表（含嵌套子回复）
     */
    @Override
    public List<CommentVO> listComments(Long postId, int page, int size, Long currentUserId) {
        // 查询所有正常评论（不分父子，按时间排序）
        LambdaQueryWrapper<CommentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPO::getPostId, postId)
                .eq(CommentPO::getStatus, 1)
                .orderByAsc(CommentPO::getCreateTime);
        List<CommentPO> allComments = commentMapper.selectList(wrapper);

        if (CollUtil.isEmpty(allComments)) {
            return Collections.emptyList();
        }

        // 收集所有用户 ID
        Set<Long> userIds = new HashSet<>();
        for (CommentPO c : allComments) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) {
                userIds.add(c.getReplyToUserId());
            }
        }

        // 批量查询用户信息
        Map<Long, UserProfileVO> userMap = getUserMap(userIds);

        // 查询当前用户的点赞记录
        Set<Long> likedCommentIds = getLikedCommentIds(currentUserId);

        // 转换为 VO
        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        for (CommentPO po : allComments) {
            CommentVO vo = toVO(po, userMap, likedCommentIds);
            voMap.put(vo.getId(), vo);
        }

        // 构建父子关系
        List<CommentVO> topLevel = new ArrayList<>();
        for (CommentPO po : allComments) {
            CommentVO vo = voMap.get(po.getId());
            if (po.getParentId() == null) {
                topLevel.add(vo);
            } else {
                CommentVO parent = voMap.get(po.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }

        // 分页：只对顶级评论分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, topLevel.size());
        if (start >= topLevel.size()) {
            return Collections.emptyList();
        }
        return topLevel.subList(start, end);
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
            // 已点赞 → 取消点赞
            wrapper.setSql("likes = likes - 1");
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
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        Map<Long, UserProfileVO> map = new HashMap<>();
        for (Long uid : userIds) {
            try {
                Result<UserProfileVO> result = userFeignClient.getById(uid);
                if (result != null && result.getData() != null) {
                    map.put(uid, result.getData());
                }
            } catch (Exception e) {
                log.warn("查询用户信息失败: userId={}", uid, e);
            }
        }
        return map;
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
