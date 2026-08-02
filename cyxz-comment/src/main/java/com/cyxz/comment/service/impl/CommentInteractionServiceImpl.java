package com.cyxz.comment.service.impl;

import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentLikeMapper;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentInteractionService;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * <p>UPSERT 一条 SQL 完成：rows=1 新增, rows=2 恢复, rows=0 幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        int rows = commentLikeMapper.upsertLike(commentId, userId);
        if (rows > 0) {
            incrementLikeDelta(commentId, 1);
            log.info("点赞评论{}: commentId={}, userId={}", rows == 1 ? "" : "(恢复)", commentId, userId);
        }
    }

    /**
     * 取消点赞评论（幂等，并发安全）
     * <p>条件 UPDATE：仅 status=1 时更新为 0，一条 SQL 搞定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long userId, Long commentId) {
        CommentPO po = commentMapper.selectById(commentId);
        if (po == null || po.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        int rows = commentLikeMapper.deactivateLike(commentId, userId);
        if (rows > 0) {
            incrementLikeDelta(commentId, -1);
            log.info("取消点赞评论: commentId={}, userId={}", commentId, userId);
        }
    }

    private void incrementLikeDelta(Long commentId, int delta) {
        stringRedisTemplate.opsForHash()
                .increment(CacheKeyConstants.COMMENT_LIKE_DELTA, commentId.toString(), delta);
    }
}
