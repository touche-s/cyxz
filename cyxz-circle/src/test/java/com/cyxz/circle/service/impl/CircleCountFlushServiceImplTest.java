package com.cyxz.circle.service.impl;

import com.cyxz.circle.entity.CirclePO;
import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.common.base.Result;
import com.cyxz.post.feign.PostFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CircleCountFlushServiceImpl 单元测试
 * <p>覆盖从 post 服务批量拉取帖子数并覆盖写入 circle.post_count 的各类场景。
 * <p>selectList(null) 不涉及 LambdaQueryWrapper，纯单测环境可安全执行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CircleCountFlushServiceImpl 圈子计数刷库")
class CircleCountFlushServiceImplTest {

    @Mock
    private CircleMapper circleMapper;

    @Mock
    private PostFeignClient postFeignClient;

    @InjectMocks
    private CircleCountFlushServiceImpl circleCountFlushService;

    private CirclePO buildCircle(Long id) {
        CirclePO po = new CirclePO();
        po.setId(id);
        po.setName("圈子" + id);
        return po;
    }

    // ==================== flushPostCounts ====================

    @Nested
    @DisplayName("flushPostCounts — 刷新圈子帖子数")
    class FlushPostCounts {

        @Test
        @DisplayName("无圈子返回 0")
        void shouldReturnZeroWhenNoCircles() {
            when(circleMapper.selectList(any())).thenReturn(List.of());

            int result = circleCountFlushService.flushPostCounts();

            assertEquals(0, result);
            verify(postFeignClient, never()).batchCountByCircle(any());
        }

        @Test
        @DisplayName("Feign 返回 null 返回 0")
        void shouldReturnZeroWhenFeignReturnsNull() {
            when(circleMapper.selectList(any())).thenReturn(List.of(buildCircle(1L)));
            when(postFeignClient.batchCountByCircle(any())).thenReturn(null);

            int result = circleCountFlushService.flushPostCounts();

            assertEquals(0, result);
            verify(circleMapper, never()).setPostCount(anyLong(), anyInt());
        }

        @Test
        @DisplayName("正常批量更新帖子数")
        void shouldBatchUpdatePostCounts() {
            when(circleMapper.selectList(any()))
                    .thenReturn(List.of(buildCircle(1L), buildCircle(2L)));
            Map<Long, Integer> counts = new HashMap<>();
            counts.put(1L, 10);
            counts.put(2L, 20);
            when(postFeignClient.batchCountByCircle(any()))
                    .thenReturn(Result.success(counts));

            int result = circleCountFlushService.flushPostCounts();

            assertEquals(2, result);
            verify(circleMapper).setPostCount(1L, 10);
            verify(circleMapper).setPostCount(2L, 20);
        }

        @Test
        @DisplayName("部分失败仍返回成功数")
        void shouldReturnSuccessCountWhenPartialFailure() {
            when(circleMapper.selectList(any()))
                    .thenReturn(List.of(buildCircle(1L), buildCircle(2L)));
            Map<Long, Integer> counts = new HashMap<>();
            counts.put(1L, 10);
            counts.put(2L, 20);
            when(postFeignClient.batchCountByCircle(any()))
                    .thenReturn(Result.success(counts));
            doThrow(new RuntimeException("DB 异常"))
                    .when(circleMapper).setPostCount(1L, 10);

            int result = circleCountFlushService.flushPostCounts();

            assertEquals(1, result);
            verify(circleMapper).setPostCount(1L, 10);
            verify(circleMapper).setPostCount(2L, 20);
        }
    }
}
