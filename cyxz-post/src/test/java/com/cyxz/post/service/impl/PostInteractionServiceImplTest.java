package com.cyxz.post.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.PostStatus;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PostInteractionServiceImpl 单元测试
 * <p>覆盖点赞/收藏的幂等性（rows=1 新增 / rows=2 恢复 / rows=0 幂等）和浏览去重。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostInteractionServiceImpl 帖子互动")
class PostInteractionServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private PostLikeMapper postLikeMapper;
    @Mock private PostCollectMapper postCollectMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PostInteractionServiceImpl interactionService;

    @Mock private HashOperations<String, Object, Object> hashOps;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private HttpServletRequest request;

    private static final Long USER_ID = 100L;
    private static final Long POST_ID = 1000L;

    private PostPO buildPost(int status) {
        PostPO po = new PostPO();
        po.setId(POST_ID);
        po.setUserId(200L); // 作者非当前用户
        po.setStatus(status);
        return po;
    }

    // ==================== 点赞 ====================

    @Nested
    @DisplayName("likePost — 点赞幂等")
    class LikePost {

        @Test
        @DisplayName("新增点赞：rows=1 计数+1 并发通知")
        void shouldIncrementAndNotifyOnNewLike() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postLikeMapper.upsertLike(POST_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.likePost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_LIKE_DELTA, POST_ID.toString(), 1);
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("恢复点赞：rows=2 计数+1 但不发通知")
        void shouldIncrementWithoutNotifyOnRestoreLike() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postLikeMapper.upsertLike(POST_ID, USER_ID)).thenReturn(2);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.likePost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_LIKE_DELTA, POST_ID.toString(), 1);
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("幂等点赞：rows=0 不计数不发通知")
        void shouldDoNothingOnIdempotentLike() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postLikeMapper.upsertLike(POST_ID, USER_ID)).thenReturn(0);

            interactionService.likePost(USER_ID, POST_ID);

            verify(stringRedisTemplate, never()).opsForHash();
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("非已发布帖子不可点赞")
        void shouldRejectLikeOnNonApprovedPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DRAFT));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.likePost(USER_ID, POST_ID));

            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode());
            verify(postLikeMapper, never()).upsertLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("帖子不存在不可点赞")
        void shouldRejectLikeOnMissingPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.likePost(USER_ID, POST_ID));

            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("unlikePost — 取消点赞幂等")
    class UnlikePost {

        @Test
        @DisplayName("取消有效点赞：rows>0 计数-1")
        void shouldDecrementOnSuccessfulUnlike() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postLikeMapper.deactivateLike(POST_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.unlikePost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_LIKE_DELTA, POST_ID.toString(), -1);
        }

        @Test
        @DisplayName("幂等取消：rows=0 不计数")
        void shouldDoNothingOnIdempotentUnlike() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postLikeMapper.deactivateLike(POST_ID, USER_ID)).thenReturn(0);

            interactionService.unlikePost(USER_ID, POST_ID);

            verify(stringRedisTemplate, never()).opsForHash();
        }

        @Test
        @DisplayName("非已发布帖子不可取消点赞")
        void shouldRejectUnlikeOnNonApprovedPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DELETED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.unlikePost(USER_ID, POST_ID));

            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode());
            verify(postLikeMapper, never()).deactivateLike(anyLong(), anyLong());
        }
    }

    // ==================== 收藏 ====================

    @Nested
    @DisplayName("collectPost — 收藏幂等")
    class CollectPost {

        @Test
        @DisplayName("新增收藏：rows=1 计数+1")
        void shouldIncrementOnNewCollect() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postCollectMapper.upsertCollect(POST_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.collectPost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_COLLECT_DELTA, POST_ID.toString(), 1);
        }

        @Test
        @DisplayName("恢复收藏：rows=2 计数+1")
        void shouldIncrementOnRestoreCollect() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postCollectMapper.upsertCollect(POST_ID, USER_ID)).thenReturn(2);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.collectPost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_COLLECT_DELTA, POST_ID.toString(), 1);
        }

        @Test
        @DisplayName("幂等收藏：rows=0 不计数")
        void shouldDoNothingOnIdempotentCollect() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postCollectMapper.upsertCollect(POST_ID, USER_ID)).thenReturn(0);

            interactionService.collectPost(USER_ID, POST_ID);

            verify(stringRedisTemplate, never()).opsForHash();
        }

        @Test
        @DisplayName("非已发布帖子不可收藏")
        void shouldRejectCollectOnNonApprovedPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.REJECTED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interactionService.collectPost(USER_ID, POST_ID));

            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode());
            verify(postCollectMapper, never()).upsertCollect(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("uncollectPost — 取消收藏幂等")
    class UncollectPost {

        @Test
        @DisplayName("取消有效收藏：rows>0 计数-1")
        void shouldDecrementOnSuccessfulUncollect() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postCollectMapper.deactivateCollect(POST_ID, USER_ID)).thenReturn(1);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.uncollectPost(USER_ID, POST_ID);

            verify(hashOps).increment(CacheKeyConstants.POST_COLLECT_DELTA, POST_ID.toString(), -1);
        }

        @Test
        @DisplayName("幂等取消收藏：rows=0 不计数")
        void shouldDoNothingOnIdempotentUncollect() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(postCollectMapper.deactivateCollect(POST_ID, USER_ID)).thenReturn(0);

            interactionService.uncollectPost(USER_ID, POST_ID);

            verify(stringRedisTemplate, never()).opsForHash();
        }
    }

    // ==================== 浏览去重 ====================

    @Nested
    @DisplayName("recordView — 浏览去重")
    class RecordView {

        @Test
        @DisplayName("首次浏览：去重 key 设置成功，浏览增量+1")
        void shouldIncrementOnFirstView() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(true);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.recordView(POST_ID, USER_ID, request);

            verify(hashOps).increment(CacheKeyConstants.POST_VIEW_DELTA, POST_ID.toString(), 1);
        }

        @Test
        @DisplayName("重复浏览：去重 key 已存在，不计数")
        void shouldNotIncrementOnDuplicateView() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(false);

            interactionService.recordView(POST_ID, USER_ID, request);

            verify(stringRedisTemplate, never()).opsForHash();
        }

        @Test
        @DisplayName("非已发布帖子不计浏览")
        void shouldNotCountViewForNonApprovedPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DRAFT));

            interactionService.recordView(POST_ID, USER_ID, request);

            verify(stringRedisTemplate, never()).opsForValue();
            verify(stringRedisTemplate, never()).opsForHash();
        }

        @Test
        @DisplayName("帖子不存在不计浏览")
        void shouldNotCountViewForMissingPost() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            interactionService.recordView(POST_ID, USER_ID, request);

            verify(stringRedisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("登录用户用 userId 作为去重标识")
        void shouldUseUserIdAsIdentityForLoggedInUser() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(true);
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

            interactionService.recordView(POST_ID, USER_ID, request);

            // 去重 key 包含 "user:100"
            verify(valueOps).setIfAbsent(contains("user:100"), eq("1"), any(Duration.class));
        }
    }
}
