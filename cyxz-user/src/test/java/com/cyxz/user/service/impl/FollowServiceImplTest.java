package com.cyxz.user.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.message.constant.NotificationConstants;
import com.cyxz.user.entity.UserFollowPO;
import com.cyxz.user.mapper.UserFollowMapper;
import com.cyxz.user.vo.FollowUserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FollowServiceImpl 单元测试
 * <p>覆盖关注/取关幂等性、自关注拦截、互相关注判定、列表查询等场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FollowServiceImpl 关注服务")
class FollowServiceImplTest {

    @Mock private UserFollowMapper followMapper;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FollowServiceImpl followService;

    private static final Long USER_ID = 100L;
    private static final Long TARGET_ID = 200L;

    // ==================== follow ====================

    @Nested
    @DisplayName("follow — 关注用户")
    class Follow {

        @Test
        @DisplayName("自关注被拒")
        void shouldRejectSelfFollow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> followService.follow(USER_ID, USER_ID));

            assertEquals(ErrorCode.SELF_OPERATION_FORBIDDEN.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("不能关注自己"));
            verify(followMapper, never()).upsertFollow(anyLong(), anyLong());
        }

        @Test
        @DisplayName("新关注：rows=1 发送 MQ 通知")
        void shouldSendNotificationOnNewFollow() {
            when(followMapper.upsertFollow(USER_ID, TARGET_ID)).thenReturn(1);

            followService.follow(USER_ID, TARGET_ID);

            verify(rabbitTemplate).convertAndSend(
                    eq(NotificationConstants.EXCHANGE),
                    eq(NotificationConstants.ROUTING_KEY),
                    any(Object.class));
        }

        @Test
        @DisplayName("恢复关注：rows=2 不发通知")
        void shouldNotSendNotificationOnRestoreFollow() {
            when(followMapper.upsertFollow(USER_ID, TARGET_ID)).thenReturn(2);

            followService.follow(USER_ID, TARGET_ID);

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("幂等关注：rows=0 不发通知")
        void shouldNotSendNotificationOnIdempotentFollow() {
            when(followMapper.upsertFollow(USER_ID, TARGET_ID)).thenReturn(0);

            followService.follow(USER_ID, TARGET_ID);

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("MQ 发送失败不影响关注结果")
        void shouldNotFailWhenMqDown() {
            when(followMapper.upsertFollow(USER_ID, TARGET_ID)).thenReturn(1);
            doThrow(new RuntimeException("MQ down"))
                    .when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(Object.class));

            // MQ 异常被 catch，不传播
            assertDoesNotThrow(() -> followService.follow(USER_ID, TARGET_ID));
        }
    }

    // ==================== unfollow ====================

    @Nested
    @DisplayName("unfollow — 取消关注")
    class Unfollow {

        @Test
        @DisplayName("有效取关：rows>0 正常完成")
        void shouldSucceedOnValidUnfollow() {
            when(followMapper.deactivateFollow(USER_ID, TARGET_ID)).thenReturn(1);

            assertDoesNotThrow(() -> followService.unfollow(USER_ID, TARGET_ID));
        }

        @Test
        @DisplayName("幂等取关：rows=0 不报错")
        void shouldNotFailOnIdempotentUnfollow() {
            when(followMapper.deactivateFollow(USER_ID, TARGET_ID)).thenReturn(0);

            assertDoesNotThrow(() -> followService.unfollow(USER_ID, TARGET_ID));
        }
    }

    // ==================== isFollowing / isMutualFollowing ====================

    @Nested
    @DisplayName("isFollowing / isMutualFollowing — 关注状态查询")
    class FollowStatus {

        @Test
        @DisplayName("已关注返回 true")
        void shouldReturnTrueWhenFollowing() {
            when(followMapper.selectCount(any())).thenReturn(1L);

            assertTrue(followService.isFollowing(USER_ID, TARGET_ID));
        }

        @Test
        @DisplayName("未关注返回 false")
        void shouldReturnFalseWhenNotFollowing() {
            when(followMapper.selectCount(any())).thenReturn(0L);

            assertFalse(followService.isFollowing(USER_ID, TARGET_ID));
        }

        @Test
        @DisplayName("互相关注：双向都关注返回 true")
        void shouldReturnTrueForMutualFollowing() {
            when(followMapper.selectCount(any())).thenReturn(1L);

            assertTrue(followService.isMutualFollowing(USER_ID, TARGET_ID));
        }

        @Test
        @DisplayName("单向关注不算互相关注")
        void shouldReturnFalseForOneWayFollow() {
            // 第一次查询（USER→TARGET）返回 1，第二次（TARGET→USER）返回 0
            when(followMapper.selectCount(any()))
                    .thenReturn(1L)
                    .thenReturn(0L);

            assertFalse(followService.isMutualFollowing(USER_ID, TARGET_ID));
        }
    }

    // ==================== 统计与列表 ====================

    @Nested
    @DisplayName("count / list — 统计与列表查询")
    class CountAndList {

        @Test
        @DisplayName("countFollowing 委托 mapper")
        void shouldDelegateCountFollowing() {
            when(followMapper.countFollowing(USER_ID)).thenReturn(42);

            assertEquals(42, followService.countFollowing(USER_ID));
        }

        @Test
        @DisplayName("countFollowers 委托 mapper")
        void shouldDelegateCountFollowers() {
            when(followMapper.countFollowers(USER_ID)).thenReturn(99);

            assertEquals(99, followService.countFollowers(USER_ID));
        }

        @Test
        @DisplayName("listFollowing：total=0 返回空页")
        void shouldReturnEmptyPageWhenNoFollowing() {
            when(followMapper.countFollowing(USER_ID)).thenReturn(0);

            PageResult<FollowUserVO> result = followService.listFollowing(USER_ID, 1, 20);

            assertTrue(result.getRecords().isEmpty());
            verify(followMapper, never()).selectFollowingPage(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("listFollowing：返回列表且 following 字段全部为 true")
        void shouldReturnFollowingListWithFollowingTrue() {
            when(followMapper.countFollowing(USER_ID)).thenReturn(2);
            FollowUserVO vo1 = new FollowUserVO();
            vo1.setUserId(TARGET_ID);
            vo1.setNickname("用户A");
            FollowUserVO vo2 = new FollowUserVO();
            vo2.setUserId(300L);
            vo2.setNickname("用户B");
            when(followMapper.selectFollowingPage(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(List.of(vo1, vo2));

            PageResult<FollowUserVO> result = followService.listFollowing(USER_ID, 1, 20);

            assertEquals(2, result.getRecords().size());
            assertTrue(result.getRecords().get(0).getFollowing());
            assertTrue(result.getRecords().get(1).getFollowing());
        }

        @Test
        @DisplayName("listFollowers：total=0 返回空页")
        void shouldReturnEmptyPageWhenNoFollowers() {
            when(followMapper.countFollowers(USER_ID)).thenReturn(0);

            PageResult<FollowUserVO> result = followService.listFollowers(USER_ID, 1, 20);

            assertTrue(result.getRecords().isEmpty());
            verify(followMapper, never()).selectFollowersPage(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("listFollowers：根据已关注列表补 following 字段")
        void shouldFillFollowingFieldForFollowers() {
            when(followMapper.countFollowers(USER_ID)).thenReturn(2);
            FollowUserVO fan1 = new FollowUserVO();
            fan1.setUserId(TARGET_ID);  // 已回关
            fan1.setNickname("粉丝A");
            FollowUserVO fan2 = new FollowUserVO();
            fan2.setUserId(300L);  // 未回关
            fan2.setNickname("粉丝B");
            when(followMapper.selectFollowersPage(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(List.of(fan1, fan2));
            // 当前用户已关注 TARGET_ID，未关注 300
            when(followMapper.selectFollowingIds(USER_ID)).thenReturn(List.of(TARGET_ID));

            PageResult<FollowUserVO> result = followService.listFollowers(USER_ID, 1, 20);

            assertEquals(2, result.getRecords().size());
            assertTrue(result.getRecords().get(0).getFollowing());   // fan1 已回关
            assertFalse(result.getRecords().get(1).getFollowing());  // fan2 未回关
        }

        @Test
        @DisplayName("listFollowingUserIds 委托 mapper")
        void shouldDelegateListFollowingUserIds() {
            when(followMapper.selectFollowingIds(USER_ID)).thenReturn(List.of(TARGET_ID, 300L));

            List<Long> ids = followService.listFollowingUserIds(USER_ID);

            assertEquals(2, ids.size());
            assertTrue(ids.contains(TARGET_ID));
        }

        @Test
        @DisplayName("countNewFollowers 委托 mapper")
        void shouldDelegateCountNewFollowers() {
            when(followMapper.countNewFollowers(USER_ID)).thenReturn(5);

            assertEquals(5, followService.countNewFollowers(USER_ID));
        }
    }
}
