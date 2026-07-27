package com.cyxz.comment.service.impl;

import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.common.constant.CacheKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentCountFlushService 评论点赞增量刷库")
class CommentCountFlushServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @InjectMocks
    private CommentCountFlushServiceImpl flushService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
    }

    @Nested
    @DisplayName("flushLikeCounts — 评论点赞增量")
    class FlushLikeCounts {

        @Test
        @DisplayName("正常刷入正增量，成功后扣减并删除 field")
        void shouldFlushPositiveDelta() {
            Map<Object, Object> deltas = new HashMap<>();
            deltas.put("1", "10");
            deltas.put("2", "5");
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(deltas);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "1", -10)).thenReturn(0L);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "2", -5)).thenReturn(0L);

            int success = flushService.flushLikeCounts();

            assertEquals(2, success);
            verify(commentMapper).updateLikes(1L, 10);
            verify(commentMapper).updateLikes(2L, 5);
            verify(hashOps).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "1");
            verify(hashOps).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "2");
        }

        @Test
        @DisplayName("点赞增量可正可负（取消点赞）")
        void shouldHandleLikeAndUnlike() {
            Map<Object, Object> deltas = new HashMap<>();
            deltas.put("100", "3");
            deltas.put("200", "-1");
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(deltas);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "100", -3)).thenReturn(0L);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "200", 1)).thenReturn(0L);

            int success = flushService.flushLikeCounts();

            assertEquals(2, success);
            verify(commentMapper).updateLikes(100L, 3);
            verify(commentMapper).updateLikes(200L, -1);
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ErrorScenarios {

        @Test
        @DisplayName("空 Hash 返回 0，不触发任何 DB 操作")
        void shouldReturnZeroForEmptyHash() {
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(new HashMap<>());

            int success = flushService.flushLikeCounts();

            assertEquals(0, success);
            verify(commentMapper, never()).updateLikes(anyLong(), anyInt());
        }

        @Test
        @DisplayName("格式异常 field 直接删除，不阻塞后续")
        void shouldSkipMalformedEntryAndContinue() {
            Map<Object, Object> deltas = new HashMap<>();
            deltas.put("abc", "xyz");
            deltas.put("3", "5");
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(deltas);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "3", -5)).thenReturn(0L);

            int success = flushService.flushLikeCounts();

            assertEquals(1, success);
            verify(commentMapper).updateLikes(3L, 5);
            verify(hashOps).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "abc");
            verify(hashOps).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "3");
        }

        @Test
        @DisplayName("DB 更新失败保留增量不删，成功条目正常扣减")
        void shouldRetainDeltaOnUpdateFailure() {
            Map<Object, Object> deltas = new HashMap<>();
            deltas.put("1", "10");
            deltas.put("2", "5");
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(deltas);
            doThrow(new RuntimeException("DB 宕机"))
                    .when(commentMapper).updateLikes(1L, 10);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "2", -5)).thenReturn(0L);

            int success = flushService.flushLikeCounts();

            assertEquals(1, success);
            verify(hashOps, never()).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "1");
            verify(hashOps).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "2");
        }

        @Test
        @DisplayName("刷库期间并发写入新增量，剩余量 > 0 时不删除 field")
        void shouldRetainFieldWhenNewDeltaWrittenDuringFlush() {
            Map<Object, Object> deltas = new HashMap<>();
            deltas.put("1", "10");
            when(hashOps.entries(CacheKeyConstants.COMMENT_LIKE_DELTA)).thenReturn(deltas);
            when(hashOps.increment(CacheKeyConstants.COMMENT_LIKE_DELTA, "1", -10)).thenReturn(3L);

            int success = flushService.flushLikeCounts();

            assertEquals(1, success);
            verify(commentMapper).updateLikes(1L, 10);
            verify(hashOps, never()).delete(CacheKeyConstants.COMMENT_LIKE_DELTA, "1");
        }
    }
}
