package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.AiReviewService.AiReviewResult;
import com.cyxz.post.service.AiReviewService;
import com.cyxz.post.service.SensitiveWordService;
import com.cyxz.user.feign.UserFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PostServiceImpl 单元测试
 * <p>覆盖帖子 CRUD、状态机 CAS 并发、置顶原子限制、批量操作、内部接口等核心场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl 帖子核心服务")
class PostServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private CircleFeignClient circleFeignClient;
    @Mock private PostLikeMapper postLikeMapper;
    @Mock private PostCollectMapper postCollectMapper;
    @Mock private CommentFeignClient commentFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private SensitiveWordService sensitiveWordService;
    @Mock private AiReviewService aiReviewService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PostServiceImpl postService;

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long POST_ID = 1000L;
    private static final Long CIRCLE_ID = 7L;

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段
        ReflectionTestUtils.setField(postService, "cacheTtlMinutes", 30L);
        // 手动开启事务同步，供 hardDeletePost / batchOperate 注册 afterCommit 回调
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ==================== 辅助方法 ====================

    private CreatePostRequest buildPublishRequest() {
        CreatePostRequest req = new CreatePostRequest();
        req.setCircleId(CIRCLE_ID);
        req.setTitle("测试标题");
        req.setContent("测试正文");
        req.setImages(List.of("https://example.com/1.png"));
        req.setPostType("NORMAL");
        req.setStatus(PostStatus.PENDING);
        return req;
    }

    private CreatePostRequest buildDraftRequest() {
        CreatePostRequest req = new CreatePostRequest();
        req.setCircleId(CIRCLE_ID);
        req.setTitle("草稿标题");
        req.setContent("草稿正文");
        req.setPostType("NORMAL");
        req.setStatus(PostStatus.DRAFT);
        return req;
    }

    private PostPO buildPost(int status) {
        PostPO po = new PostPO();
        po.setId(POST_ID);
        po.setUserId(USER_ID);
        po.setCircleId(CIRCLE_ID);
        po.setTitle("测试帖子");
        po.setContent("内容");
        po.setStatus(status);
        po.setIsPinned(0);
        return po;
    }

    private void mockCirclePublishable(Long circleId, Long userId, boolean ok) {
        PublishableResult data = new PublishableResult();
        data.setExists(true);
        data.setEnabled(true);
        data.setJoined(ok);
        data.setPublishable(ok);
        when(circleFeignClient.checkPublishable(circleId, userId)).thenReturn(Result.success(data));
    }

    // ==================== createPost ====================

    @Nested
    @DisplayName("createPost — 创建帖子")
    class CreatePost {

        @Test
        @DisplayName("草稿：仅校验有内容，不校验发布字段、不触发敏感词、不触发 AI 审核")
        void shouldCreateDraftWithoutPublishValidation() {
            CreatePostRequest req = buildDraftRequest();

            postService.createPost(USER_ID, req);

            verify(sensitiveWordService, never()).check(any(), any());
            verify(aiReviewService, never()).review(anyLong(), any(), any(), any());
            verify(postMapper).insert(argThat(po -> po.getStatus() == PostStatus.DRAFT));
        }

        @Test
        @DisplayName("发布：校验通过后插入 PENDING 帖子，异步触发 AI 审核")
        void shouldCreatePendingPostAndTriggerAiReview() {
            CreatePostRequest req = buildPublishRequest();
            when(sensitiveWordService.check(any(), any())).thenReturn(Collections.emptySet());
            mockCirclePublishable(CIRCLE_ID, USER_ID, true);
            // 模拟 insert 后 id 回填
            doAnswer(inv -> {
                PostPO po = inv.getArgument(0);
                po.setId(POST_ID);
                return 1;
            }).when(postMapper).insert(any(PostPO.class));

            Long result = postService.createPost(USER_ID, req);

            assertEquals(POST_ID, result);
            verify(sensitiveWordService).check("测试标题", "测试正文");
            verify(circleFeignClient).checkPublishable(CIRCLE_ID, USER_ID);
            verify(postMapper).insert(argThat(po -> po.getStatus() == PostStatus.PENDING));
            // AI 审核是异步调用，无法直接 verify，但可以验证 mapper.insert 已触发
        }

        @Test
        @DisplayName("发布：命中敏感词直接拒绝，不写库")
        void shouldRejectPublishWithSensitiveWords() {
            CreatePostRequest req = buildPublishRequest();
            mockCirclePublishable(CIRCLE_ID, USER_ID, true);
            when(sensitiveWordService.check(any(), any())).thenReturn(Set.of("违规词"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.createPost(USER_ID, req));

            verify(postMapper, never()).insert(any(PostPO.class));
            assertEquals(ErrorCode.CONTENT_SENSITIVE.getCode(), ex.getCode());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("发布：非圈子成员被拒")
        void shouldRejectPublishWhenNotCircleMember() {
            CreatePostRequest req = buildPublishRequest();
            mockCirclePublishable(CIRCLE_ID, USER_ID, false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.createPost(USER_ID, req));

            verify(postMapper, never()).insert(any(PostPO.class));
            assertTrue(ex.getMessage().contains("请先加入该圈子"));
        }

        @Test
        @DisplayName("发布：图文帖缺少图片被拒")
        void shouldRejectNormalPostWithoutImages() {
            CreatePostRequest req = buildPublishRequest();
            req.setImages(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.createPost(USER_ID, req));

            verify(postMapper, never()).insert(any(PostPO.class));
            assertTrue(ex.getMessage().contains("至少需要一张图片"));
        }

        @Test
        @DisplayName("发布：长文帖免图片校验")
        void shouldAllowArticleWithoutImages() {
            CreatePostRequest req = buildPublishRequest();
            req.setPostType("ARTICLE");
            req.setImages(null);
            req.setContent("a".repeat(150)); // 长文至少 100 字
            when(sensitiveWordService.check(any(), any())).thenReturn(Collections.emptySet());
            mockCirclePublishable(CIRCLE_ID, USER_ID, true);

            assertDoesNotThrow(() -> postService.createPost(USER_ID, req));
            verify(postMapper).insert(any(PostPO.class));
        }

        @Test
        @DisplayName("草稿：缺少正文和图片被拒")
        void shouldRejectDraftWithoutAnyContent() {
            CreatePostRequest req = buildDraftRequest();
            req.setContent(null);
            req.setImages(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.createPost(USER_ID, req));

            verify(postMapper, never()).insert(any(PostPO.class));
            assertTrue(ex.getMessage().contains("至少需要正文或图片"));
        }
    }

    // ==================== updatePost（CAS 并发） ====================

    @Nested
    @DisplayName("updatePost — 更新帖子与 CAS 并发控制")
    class UpdatePost {

        @Test
        @DisplayName("帖子不存在抛 POST_NOT_FOUND")
        void shouldThrowWhenPostNotFound() {
            UpdatePostRequest req = new UpdatePostRequest();
            req.setId(POST_ID.toString());
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(USER_ID, req));
            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非作者抛 UNAUTHORIZED")
        void shouldThrowWhenNotAuthor() {
            UpdatePostRequest req = new UpdatePostRequest();
            req.setId(POST_ID.toString());
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DRAFT));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(OTHER_USER_ID, req));
            assertEquals(ErrorCode.NOT_POST_OWNER.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("CAS 更新成功：rows=1 正常完成")
        void shouldUpdateSuccessfullyWhenCasMatch() {
            UpdatePostRequest req = new UpdatePostRequest();
            req.setId(POST_ID.toString());
            req.setTitle("新标题");
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DRAFT));
            when(postMapper.update(any(PostPO.class), any(Wrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> postService.updatePost(USER_ID, req));

            verify(postMapper).update(any(PostPO.class), any(Wrapper.class));
            verify(redisTemplate).delete(any(String.class));
        }

        @Test
        @DisplayName("CAS 更新失败：rows=0 抛状态已被修改，防止并发覆盖")
        void shouldThrowWhenCasMiss() {
            UpdatePostRequest req = new UpdatePostRequest();
            req.setId(POST_ID.toString());
            req.setTitle("新标题");
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.DRAFT));
            // 模拟并发：另一请求已把 DRAFT 改成 PENDING，CAS 条件不匹配
            when(postMapper.update(any(PostPO.class), any(Wrapper.class))).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(USER_ID, req));

            assertTrue(ex.getMessage().contains("帖子状态已被修改"));
            // 失败不应清缓存
            verify(redisTemplate, never()).delete(any(String.class));
        }

        @Test
        @DisplayName("非法状态迁移被拒：REJECTED → APPROVED 不允许直接通过")
        void shouldRejectIllegalTransition() {
            UpdatePostRequest req = new UpdatePostRequest();
            req.setId(POST_ID.toString());
            req.setStatus(PostStatus.APPROVED); // REJECTED → APPROVED 非法（需经 PENDING）
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.REJECTED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(USER_ID, req));

            assertTrue(ex.getMessage().contains("不允许从"));
            verify(postMapper, never()).update(any(PostPO.class), any(Wrapper.class));
        }
    }

    // ==================== deletePost / hardDeletePost ====================

    @Nested
    @DisplayName("deletePost / hardDeletePost — 帖子删除")
    class DeletePost {

        @Test
        @DisplayName("软删除：作者可删，状态改为 DELETED")
        void shouldSoftDeleteByAuthor() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            postService.deletePost(USER_ID, POST_ID);

            assertEquals(PostStatus.DELETED, po.getStatus());
            verify(postMapper).updateById(po);
            verify(redisTemplate).delete(any(String.class));
        }

        @Test
        @DisplayName("软删除：非作者被拒")
        void shouldRejectDeleteByNonAuthor() {
            when(postMapper.selectById(POST_ID)).thenReturn(buildPost(PostStatus.APPROVED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.deletePost(OTHER_USER_ID, POST_ID));
            assertEquals(ErrorCode.NOT_POST_OWNER.getCode(), ex.getCode());
            verify(postMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("彻底删除：仅回收站帖子可彻底删除")
        void shouldHardDeleteOnlyForDeletedPost() {
            PostPO po = buildPost(PostStatus.DELETED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            postService.hardDeletePost(USER_ID, POST_ID);

            verify(postLikeMapper).delete(any(Wrapper.class));
            verify(postCollectMapper).delete(any(Wrapper.class));
            verify(postMapper).deleteById(POST_ID);
        }

        @Test
        @DisplayName("彻底删除：非回收站帖子被拒")
        void shouldRejectHardDeleteForNonDeletedPost() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.hardDeletePost(USER_ID, POST_ID));
            assertTrue(ex.getMessage().contains("仅回收站中的帖子"));
            verify(postMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("彻底删除：非作者被拒")
        void shouldRejectHardDeleteByNonAuthor() {
            PostPO po = buildPost(PostStatus.DELETED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.hardDeletePost(OTHER_USER_ID, POST_ID));
            assertEquals(ErrorCode.NOT_POST_OWNER.getCode(), ex.getCode());
        }
    }

    // ==================== pinPost（原子上限） ====================

    @Nested
    @DisplayName("pinPost — 原子置顶与上限校验")
    class PinPost {

        @Test
        @DisplayName("正常置顶：rows=1 成功")
        void shouldPinSuccessfully() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);
            when(postMapper.pinPost(USER_ID, POST_ID)).thenReturn(1);

            assertDoesNotThrow(() -> postService.pinPost(USER_ID, POST_ID));
            verify(postMapper).pinPost(USER_ID, POST_ID);
        }

        @Test
        @DisplayName("已达 3 条上限：rows=0 抛最多置顶 3 条")
        void shouldRejectWhenReachLimit() {
            PostPO po = buildPost(PostStatus.APPROVED);
            po.setIsPinned(0);
            when(postMapper.selectById(POST_ID)).thenReturn(po);
            when(postMapper.pinPost(USER_ID, POST_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.pinPost(USER_ID, POST_ID));
            assertTrue(ex.getMessage().contains("最多置顶 3 条帖子"));
        }

        @Test
        @DisplayName("已置顶帖子重复置顶：rows=0 抛该帖子已置顶")
        void shouldRejectWhenAlreadyPinned() {
            PostPO po = buildPost(PostStatus.APPROVED);
            po.setIsPinned(1);
            when(postMapper.selectById(POST_ID)).thenReturn(po);
            when(postMapper.pinPost(USER_ID, POST_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.pinPost(USER_ID, POST_ID));
            assertTrue(ex.getMessage().contains("该帖子已置顶"));
        }

        @Test
        @DisplayName("非已发布帖子不能置顶")
        void shouldRejectPinNonApprovedPost() {
            PostPO po = buildPost(PostStatus.DRAFT);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.pinPost(USER_ID, POST_ID));
            assertTrue(ex.getMessage().contains("仅已发布帖子可置顶"));
            verify(postMapper, never()).pinPost(anyLong(), anyLong());
        }

        @Test
        @DisplayName("非作者不能置顶")
        void shouldRejectPinByNonAuthor() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.pinPost(OTHER_USER_ID, POST_ID));
            assertEquals(ErrorCode.NOT_POST_OWNER.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("取消置顶正常调用 mapper")
        void shouldUnpinSuccessfully() {
            PostPO po = buildPost(PostStatus.APPROVED);
            po.setIsPinned(1);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            postService.unpinPost(USER_ID, POST_ID);

            verify(postMapper).unpinPost(USER_ID, POST_ID);
        }
    }

    // ==================== batchOperate ====================

    @Nested
    @DisplayName("batchOperate — 批量操作")
    class BatchOperate {

        @Test
        @DisplayName("空列表抛参数错误")
        void shouldRejectEmptyList() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.batchOperate(USER_ID, Collections.emptyList(), "delete"));
            assertTrue(ex.getMessage().contains("请选择"));
        }

        @Test
        @DisplayName("不支持的动作抛参数错误")
        void shouldRejectUnsupportedAction() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.batchOperate(USER_ID, List.of(1L), "unknown"));
            assertTrue(ex.getMessage().contains("不支持的操作类型"));
        }

        @Test
        @DisplayName("批量删除：直接调用 batchUpdateStatus")
        void shouldBatchDelete() {
            List<Long> ids = List.of(1L, 2L, 3L);

            postService.batchOperate(USER_ID, ids, "delete");

            verify(postMapper).batchUpdateStatus(eq(USER_ID), eq(ids), eq(PostStatus.DELETED), isNull());
        }

        @Test
        @DisplayName("批量发布：仅本人草稿帖且字段完整才转 PENDING")
        void shouldBatchPublishOnlyOwnDraftsWithCompleteFields() {
            PostPO ownDraft = new PostPO();
            ownDraft.setId(1L);
            ownDraft.setUserId(USER_ID);
            ownDraft.setStatus(PostStatus.DRAFT);
            ownDraft.setTitle("标题");
            ownDraft.setContent("正文");
            ownDraft.setImages("img1.png");

            PostPO otherDraft = new PostPO();
            otherDraft.setId(2L);
            otherDraft.setUserId(OTHER_USER_ID); // 非本人
            otherDraft.setStatus(PostStatus.DRAFT);
            otherDraft.setTitle("标题");
            otherDraft.setContent("正文");
            otherDraft.setImages("img1.png");

            PostPO ownIncomplete = new PostPO();
            ownIncomplete.setId(3L);
            ownIncomplete.setUserId(USER_ID);
            ownIncomplete.setStatus(PostStatus.DRAFT);
            ownIncomplete.setTitle("标题");
            ownIncomplete.setContent("正文");
            ownIncomplete.setImages(""); // 字段不全

            when(postMapper.selectBatchIds(List.of(1L, 2L, 3L)))
                    .thenReturn(List.of(ownDraft, otherDraft, ownIncomplete));

            postService.batchOperate(USER_ID, List.of(1L, 2L, 3L), "publish");

            // 仅 ownDraft(id=1) 被转 PENDING
            verify(postMapper).batchUpdateStatus(eq(USER_ID), eq(List.of(1L)), eq(PostStatus.PENDING), eq(PostStatus.DRAFT));
        }
    }

    // ==================== 内部接口 ====================

    @Nested
    @DisplayName("内部接口 — getPostInfo / batchGetPostInfo")
    class InternalApi {

        @Test
        @DisplayName("getPostInfo：帖子不存在返回空 Map")
        void shouldReturnEmptyMapWhenPostMissing() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            Map<String, Object> info = postService.getPostInfo(POST_ID);

            assertTrue(info.isEmpty());
        }

        @Test
        @DisplayName("getPostInfo：返回 postId/userId/title/circleId")
        void shouldReturnPostInfoMap() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            Map<String, Object> info = postService.getPostInfo(POST_ID);

            assertEquals(POST_ID, ((Number) info.get("postId")).longValue());
            assertEquals(USER_ID, ((Number) info.get("userId")).longValue());
            assertEquals("测试帖子", info.get("title"));
            assertEquals(CIRCLE_ID, info.get("circleId"));
        }

        @Test
        @DisplayName("batchGetPostInfo：空入参返回空列表")
        void shouldReturnEmptyForEmptyIds() {
            List<?> result = postService.batchGetPostInfo(Collections.emptySet());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("batchGetPostInfo：批量查询返回 VO 列表")
        void shouldBatchReturnPostInfo() {
            PostPO po = buildPost(PostStatus.APPROVED);
            when(postMapper.selectBatchIds(any())).thenReturn(List.of(po));

            var result = postService.batchGetPostInfo(Set.of(POST_ID));

            assertEquals(1, result.size());
            assertEquals(POST_ID, result.get(0).getPostId());
            assertEquals(USER_ID, result.get(0).getUserId());
        }
    }

    // ==================== 审核接口 ====================

    @Nested
    @DisplayName("approvePost / rejectPost — 管理员审核")
    class ReviewPost {

        @Test
        @DisplayName("审核通过：PENDING → APPROVED")
        void shouldApprovePendingPost() {
            PostPO po = buildPost(PostStatus.PENDING);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            postService.approvePost(POST_ID);

            assertEquals(PostStatus.APPROVED, po.getStatus());
            assertNull(po.getReviewReason());
            verify(postMapper).updateById(po);
        }

        @Test
        @DisplayName("审核通过：非 PENDING 状态被拒")
        void shouldRejectApproveNonPendingPost() {
            PostPO po = buildPost(PostStatus.DRAFT);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.approvePost(POST_ID));
            assertTrue(ex.getMessage().contains("不在待审核状态"));
        }

        @Test
        @DisplayName("审核拒绝：PENDING → REJECTED 并记录原因")
        void shouldRejectPendingPost() {
            PostPO po = buildPost(PostStatus.PENDING);
            when(postMapper.selectById(POST_ID)).thenReturn(po);

            postService.rejectPost(POST_ID, "内容违规");

            assertEquals(PostStatus.REJECTED, po.getStatus());
            assertEquals("内容违规", po.getReviewReason());
            verify(postMapper).updateById(po);
        }
    }

    // ==================== batchCountByCircle ====================

    @Nested
    @DisplayName("batchCountByCircle — 批量统计圈子帖子数")
    class BatchCountByCircle {

        @Test
        @DisplayName("空入参返回空 Map")
        void shouldReturnEmptyForEmptyInput() {
            Map<Long, Integer> result = postService.batchCountByCircle(Collections.emptySet());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("有帖子的圈子返回实际数，无帖子的圈子返回 0")
        void shouldReturnCountsAndZeroForEmptyCircles() {
            Map<String, Object> row = new HashMap<>();
            row.put("circle_id", CIRCLE_ID);
            row.put("cnt", 42L);
            when(postMapper.batchCountByCircleIds(Set.of(CIRCLE_ID, 99L)))
                    .thenReturn(List.of(row));

            Map<Long, Integer> result = postService.batchCountByCircle(Set.of(CIRCLE_ID, 99L));

            assertEquals(42, result.get(CIRCLE_ID));
            assertEquals(0, result.get(99L));
        }
    }
}
