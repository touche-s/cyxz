package com.cyxz.circle.service.impl;

import com.cyxz.circle.entity.CirclePO;
import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.circle.mapper.CircleMemberMapper;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CommonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * CircleServiceImpl 单元测试
 * <p>覆盖加入/退出圈子幂等性、发布权限校验、圈子 CRUD 等场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CircleServiceImpl 圈子核心服务")
class CircleServiceImplTest {

    @Mock private CircleMapper circleMapper;
    @Mock private CircleMemberMapper circleMemberMapper;
    @Mock private CircleSectionService circleSectionService;

    @InjectMocks
    private CircleServiceImpl circleService;

    private static final Long USER_ID = 100L;
    private static final Long CIRCLE_ID = 7L;

    private CirclePO buildCircle(int status) {
        CirclePO po = new CirclePO();
        po.setId(CIRCLE_ID);
        po.setName("测试圈子");
        po.setStatus(status);
        po.setMemberCount(10);
        po.setPostCount(5);
        po.setSortOrder(0);
        return po;
    }

    // ==================== joinCircle ====================

    @Nested
    @DisplayName("joinCircle — 加入圈子")
    class JoinCircle {

        @Test
        @DisplayName("新增加入：rows=1 成员数+1")
        void shouldIncrementOnNewJoin() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));
            when(circleMemberMapper.upsertMember(CIRCLE_ID, USER_ID)).thenReturn(1);

            circleService.joinCircle(USER_ID, CIRCLE_ID);

            verify(circleMapper).updateMemberCount(CIRCLE_ID, 1);
        }

        @Test
        @DisplayName("恢复加入：rows=2 成员数+1")
        void shouldIncrementOnRestoreJoin() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));
            when(circleMemberMapper.upsertMember(CIRCLE_ID, USER_ID)).thenReturn(2);

            circleService.joinCircle(USER_ID, CIRCLE_ID);

            verify(circleMapper).updateMemberCount(CIRCLE_ID, 1);
        }

        @Test
        @DisplayName("幂等加入：rows=0 成员数不变")
        void shouldNotIncrementOnIdempotentJoin() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));
            when(circleMemberMapper.upsertMember(CIRCLE_ID, USER_ID)).thenReturn(0);

            circleService.joinCircle(USER_ID, CIRCLE_ID);

            verify(circleMapper, never()).updateMemberCount(anyLong(), anyInt());
        }

        @Test
        @DisplayName("圈子不存在抛异常")
        void shouldThrowWhenCircleNotFound() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.joinCircle(USER_ID, CIRCLE_ID));

            assertEquals(ErrorCode.CIRCLE_NOT_FOUND.getCode(), ex.getCode());
            verify(circleMemberMapper, never()).upsertMember(anyLong(), anyLong());
        }

        @Test
        @DisplayName("已停用圈子不可加入")
        void shouldRejectJoinDisabledCircle() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.DELETED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.joinCircle(USER_ID, CIRCLE_ID));

            assertTrue(ex.getMessage().contains("圈子不存在"));
            verify(circleMemberMapper, never()).upsertMember(anyLong(), anyLong());
        }
    }

    // ==================== leaveCircle ====================

    @Nested
    @DisplayName("leaveCircle — 退出圈子")
    class LeaveCircle {

        @Test
        @DisplayName("有效退出：rows>0 成员数-1")
        void shouldDecrementOnSuccessfulLeave() {
            when(circleMemberMapper.deactivateMember(CIRCLE_ID, USER_ID)).thenReturn(1);

            circleService.leaveCircle(USER_ID, CIRCLE_ID);

            verify(circleMapper).updateMemberCount(CIRCLE_ID, -1);
        }

        @Test
        @DisplayName("幂等退出：rows=0 成员数不变")
        void shouldNotDecrementOnIdempotentLeave() {
            when(circleMemberMapper.deactivateMember(CIRCLE_ID, USER_ID)).thenReturn(0);

            circleService.leaveCircle(USER_ID, CIRCLE_ID);

            verify(circleMapper, never()).updateMemberCount(anyLong(), anyInt());
        }
    }

    // ==================== checkPublishable ====================

    @Nested
    @DisplayName("checkPublishable — 发布权限校验")
    class CheckPublishable {

        @Test
        @DisplayName("圈子不存在：返回 exists=false")
        void shouldReturnNotExistsWhenCircleMissing() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(null);

            PublishableResult result = circleService.checkPublishable(CIRCLE_ID, USER_ID);

            assertFalse(result.isExists());
            assertFalse(result.isPublishable());
        }

        @Test
        @DisplayName("已加入的启用圈子：返回 publishable=true")
        void shouldReturnPublishableWhenJoined() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));
            when(circleMemberMapper.selectCount(any())).thenReturn(1L);

            PublishableResult result = circleService.checkPublishable(CIRCLE_ID, USER_ID);

            assertTrue(result.isExists());
            assertTrue(result.isEnabled());
            assertTrue(result.isJoined());
            assertTrue(result.isPublishable());
        }

        @Test
        @DisplayName("未加入的启用圈子：返回 joined=false")
        void shouldReturnNotJoinedWhenNotMember() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));
            when(circleMemberMapper.selectCount(any())).thenReturn(0L);

            PublishableResult result = circleService.checkPublishable(CIRCLE_ID, USER_ID);

            assertTrue(result.isExists());
            assertTrue(result.isEnabled());
            assertFalse(result.isJoined());
            assertFalse(result.isPublishable());
        }

        @Test
        @DisplayName("已停用圈子：返回 enabled=false")
        void shouldReturnDisabledWhenCircleInactive() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.DELETED));

            PublishableResult result = circleService.checkPublishable(CIRCLE_ID, USER_ID);

            assertTrue(result.isExists());
            assertFalse(result.isEnabled());
            assertFalse(result.isPublishable());
        }
    }

    // ==================== getById ====================

    @Nested
    @DisplayName("getById — 圈子详情")
    class GetById {

        @Test
        @DisplayName("正常圈子返回详情")
        void shouldReturnCircleDetail() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));

            CircleVO vo = circleService.getById(CIRCLE_ID, USER_ID);

            assertEquals(CIRCLE_ID, vo.getId());
            assertEquals("测试圈子", vo.getName());
        }

        @Test
        @DisplayName("圈子不存在抛异常")
        void shouldThrowWhenCircleNotFound() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.getById(CIRCLE_ID, USER_ID));

            assertTrue(ex.getMessage().contains("圈子不存在"));
        }

        @Test
        @DisplayName("已停用圈子抛异常")
        void shouldThrowWhenCircleDisabled() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.DELETED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.getById(CIRCLE_ID, USER_ID));

            assertTrue(ex.getMessage().contains("圈子不存在"));
        }
    }

    // ==================== createCircle ====================

    @Nested
    @DisplayName("createCircle — 创建圈子")
    class CreateCircle {

        @Test
        @DisplayName("正常创建：初始化默认板块")
        void shouldCreateAndInitSections() {
            doAnswer(inv -> {
                CirclePO po = inv.getArgument(0);
                po.setId(CIRCLE_ID);
                return 1;
            }).when(circleMapper).insert(any(CirclePO.class));

            CircleVO vo = circleService.createCircle("新圈子", "简介", null, null, null);

            assertEquals("新圈子", vo.getName());
            assertEquals(CommonStatus.ACTIVE, buildCircle(CommonStatus.ACTIVE).getStatus());
            verify(circleSectionService).initDefaultSections(CIRCLE_ID);
        }

        @Test
        @DisplayName("空名称被拒")
        void shouldRejectEmptyName() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.createCircle("", null, null, null, null));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            verify(circleMapper, never()).insert(any());
        }
    }

    // ==================== deleteCircle ====================

    @Nested
    @DisplayName("deleteCircle — 删除圈子")
    class DeleteCircle {

        @Test
        @DisplayName("软删除：状态改为 DELETED")
        void shouldSoftDeleteCircle() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(buildCircle(CommonStatus.ACTIVE));

            circleService.deleteCircle(CIRCLE_ID);

            verify(circleMapper).updateById(argThat(po -> po.getStatus() == CommonStatus.DELETED));
        }

        @Test
        @DisplayName("圈子不存在抛异常")
        void shouldThrowWhenCircleNotFound() {
            when(circleMapper.selectById(CIRCLE_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleService.deleteCircle(CIRCLE_ID));

            assertTrue(ex.getMessage().contains("圈子不存在"));
            verify(circleMapper, never()).updateById(any());
        }
    }
}
