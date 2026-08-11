package com.cyxz.comment.service.impl;

import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentLikeMapper;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CommentInteractionServiceImpl 单元测试
 * <p>覆盖评论点赞/取消点赞的幂等性（rows=1 新增 / rows=2 恢复 / rows=0 幂等）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentInteractionServiceImpl 评论互动")
class CommentInteractionServiceImplTest {

    @Mock private CommentMapper commentMapper;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private HashOperations<String, Object, Object> hashOps;

    @InjectMocks
    private CommentInteractionServiceImpl interactionService;

    private static final Long USER_ID = 100L;
    private static final Long COMMENT_ID = 1000L;

    private CommentPO buildComment(int status) {
        CommentPO po = new CommentPO();
        po.setId(COMMENT_ID);
        po.setPostId(500L);
        po.setUserId(200L);
        po.setStatus(status);
        return po;
    }

    // ==================== likeComment ====================

    @Nested
    @DisplayName("likeComment — 点赞评论")
    class LikeComment {

        @Test
        @DisplayName("评论不存在抛 COMMENT_NOT_FOUND")
        void shouldThrowWhenCommentNotFound() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.likeComment(USER_ID, COMMENT_ID));

            assertEquals(ErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
            verify(commentLikeMapper, never()).upsertLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("评论已删除抛 COMMENT_NOT_FOUND")
        void shouldThrowWhenCommentDeleted() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.DELETED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.likeComment(USER_ID, COMMENT_ID));

            assertEquals(ErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
            verify(commentLikeMapper, never()).upsertLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("新增点赞：rows=1 计数+1")
        void shouldIncrementOnNewLike() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.ACTIVE));
            when(commentLikeMapper.upsertLike(COMMENT_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.likeComment(USER_ID, COMMENT_ID);

            verify(hashOps).increment(CacheKeyConstants.COMMENT_LIKE_DELTA, COMMENT_ID.toString(), 1);
        }

        @Test
        @DisplayName("恢复点赞：rows=2 计数+1")
        void shouldIncrementOnRestoreLike() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.ACTIVE));
            when(commentLikeMapper.upsertLike(COMMENT_ID, USER_ID)).thenReturn(2);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.likeComment(USER_ID, COMMENT_ID);

            verify(hashOps).increment(CacheKeyConstants.COMMENT_LIKE_DELTA, COMMENT_ID.toString(), 1);
        }

        @Test
        @DisplayName("幂等点赞：rows=0 不计数")
        void shouldDoNothingOnIdempotentLike() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.ACTIVE));
            when(commentLikeMapper.upsertLike(COMMENT_ID, USER_ID)).thenReturn(0);

            interactionService.likeComment(USER_ID, COMMENT_ID);

            verify(stringRedisTemplate, never()).opsForHash();
        }
    }

    // ==================== unlikeComment ====================

    @Nested
    @DisplayName("unlikeComment — 取消点赞评论")
    class UnlikeComment {

        @Test
        @DisplayName("评论不存在抛 COMMENT_NOT_FOUND")
        void shouldThrowWhenCommentNotFound() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.unlikeComment(USER_ID, COMMENT_ID));

            assertEquals(ErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
            verify(commentLikeMapper, never()).deactivateLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("有效取消：rows>0 计数-1")
        void shouldDecrementOnSuccessfulUnlike() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.ACTIVE));
            when(commentLikeMapper.deactivateLike(COMMENT_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.unlikeComment(USER_ID, COMMENT_ID);

            verify(hashOps).increment(CacheKeyConstants.COMMENT_LIKE_DELTA, COMMENT_ID.toString(), -1);
        }

        @Test
        @DisplayName("幂等取消：rows=0 不计数")
        void shouldDoNothingOnIdempotentUnlike() {
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(buildComment(CommonStatus.ACTIVE));
            when(commentLikeMapper.deactivateLike(COMMENT_ID, USER_ID)).thenReturn(0);

            interactionService.unlikeComment(USER_ID, COMMENT_ID);

            verify(stringRedisTemplate, never()).opsForHash();
        }
    }
}
