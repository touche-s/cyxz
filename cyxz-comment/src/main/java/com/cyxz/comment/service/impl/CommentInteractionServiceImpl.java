package com.cyxz.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.comment.entity.CommentLikePO;
import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentLikeMapper;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentInteractionService;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.utils.StatusUpdateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论互动服务实现
 * <p>管理评论点赞等互动操作。
 * <p>计数方案：关系表照常写，计数通过 Redis Hash 增量记录，由定时任务统一刷库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentInteractionServiceImpl implements CommentInteractionService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 点赞评论（幂等，并发安全）
     * <p>先查关系表，不存在则插入并记增量；已存在且状态为删除则恢复。
     * 并发下 DuplicateKeyException 回退到 CAS 更新状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        CommentLikePO exist = queryCommentLike(userId, commentId);

        if (exist == null) {
            try {
                CommentLikePO newLike = new CommentLikePO();
                newLike.setCommentId(commentId);
                newLike.setUserId(userId);
                newLike.setStatus(CommonStatus.ACTIVE);
                commentLikeMapper.insert(newLike);
                incrementLikeDelta(commentId, 1);
                log.info("点赞评论: commentId={}, userId={}", commentId, userId);
            } catch (DuplicateKeyException e) {
                CommentLikePO conflict = queryCommentLike(userId, commentId);
                if (conflict.getStatus() == 1) {
                    return;
                }
                boolean updated = StatusUpdateHelper.updateStatus(commentLikeMapper, conflict.getId(), 0, 1);
                if (updated) {
                    incrementLikeDelta(commentId, 1);
                    log.info("点赞评论(并发恢复): commentId={}, userId={}", commentId, userId);
                }
            }
            return;
        }

        if (exist.getStatus() == 0) {
            boolean updated = StatusUpdateHelper.updateStatus(commentLikeMapper, exist.getId(), 0, 1);
            if (updated) {
                incrementLikeDelta(commentId, 1);
                log.info("点赞评论(恢复): commentId={}, userId={}", commentId, userId);
            }
            return;
        }

        log.debug("点赞评论(幂等忽略): commentId={}, userId={}", commentId, userId);
    }

    /**
     * 取消点赞评论（幂等，并发安全）
     * <p>CAS 将关系表状态 1→0，成功则记 -1 增量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        CommentLikePO exist = queryCommentLike(userId, commentId);
        if (exist == null || exist.getStatus() == 0) {
            return;
        }

        boolean updated = StatusUpdateHelper.updateStatus(commentLikeMapper, exist.getId(), 1, 0);
        if (updated) {
            incrementLikeDelta(commentId, -1);
            log.info("取消点赞评论: commentId={}, userId={}", commentId, userId);
        }
    }

    private void incrementLikeDelta(Long commentId, int delta) {
        stringRedisTemplate.opsForHash()
                .increment(CacheKeyConstants.COMMENT_LIKE_DELTA, commentId.toString(), delta);
    }

    private CommentLikePO queryCommentLike(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLikePO::getUserId, userId)
                .eq(CommentLikePO::getCommentId, commentId);
        return commentLikeMapper.selectOne(wrapper);
    }
}
