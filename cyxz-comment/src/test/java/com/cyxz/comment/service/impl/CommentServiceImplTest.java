package com.cyxz.comment.service.impl;

import com.cyxz.comment.dto.CreateCommentRequest;
import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentLikeMapper;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.circle.feign.CircleFeignClient;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.message.constant.NotificationConstants;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.post.feign.PostFeignClient;
import com.cyxz.post.api.vo.PostInfoVO;
import com.cyxz.user.feign.UserFeignClient;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl 评论创建与通知")
class CommentServiceImplTest {

    @Mock private CommentMapper commentMapper;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private UserFeignClient userFeignClient;
    @Mock private PostFeignClient postFeignClient;
    @Mock private CircleFeignClient circleFeignClient;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private HashOperations<String, Object, Object> hashOps;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        PostInfoVO postInfo = new PostInfoVO();
        postInfo.setPostId(100L);
        postInfo.setUserId(999L);
        postInfo.setTitle("测试帖子");
        postInfo.setCircleId(7L);
        when(postFeignClient.getPostInfo(any())).thenReturn(Result.success(postInfo));

        PublishableResult circleData = new PublishableResult();
        circleData.setExists(true);
        circleData.setEnabled(true);
        circleData.setJoined(true);
        circleData.setPublishable(true);
        when(circleFeignClient.checkPublishable(any(), any())).thenReturn(Result.success(circleData));

        // 父评论 mock（回复类测试用，顶级评论测试不会触发）
        CommentPO parent = new CommentPO();
        parent.setId(100L);
        parent.setPostId(100L);
        parent.setStatus(CommonStatus.ACTIVE);
        lenient().when(commentMapper.selectById(100L)).thenReturn(parent);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // 模拟编程式事务：立即执行回调并触发 afterCommit（模拟事务提交成功）
        lenient().doAnswer(inv -> {
            Consumer<org.springframework.transaction.TransactionStatus> consumer = inv.getArgument(0);
            TransactionSynchronizationManager.initSynchronization();
            consumer.accept(null);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
            TransactionSynchronizationManager.clearSynchronization();
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    @DisplayName("回复通知 — COMMENT_REPLIED MQ 发布")
    class CommentRepliedNotification {

        @Test
        @DisplayName("回复他人评论时发布 MQ 事件")
        void shouldPublishMqEventWhenReplyingToOther() {
            CreateCommentRequest request = buildReplyRequest(1L, 100L, 200L);
            when(commentMapper.insert(any(CommentPO.class))).thenAnswer(inv -> {
                CommentPO po = inv.getArgument(0);
                po.setId(10L);
                return 1;
            });

            commentService.createComment(1L, request);

            verify(rabbitTemplate).convertAndSend(
                    eq(NotificationConstants.EXCHANGE),
                    eq(NotificationConstants.ROUTING_KEY),
                    argThat((NotificationEvent e) ->
                            e.getReceiverId().equals(200L) &&
                            e.getSenderId().equals(1L) &&
                            e.getType().equals(NotificationType.COMMENT_REPLIED.name()) &&
                            e.getTitle().equals("有人回复了你的评论") &&
                            e.getTargetId().equals(10L)
                    )
            );
        }

        @Test
        @DisplayName("非回复评论（一级评论）不发布 COMMENT_REPLIED 事件")
        void shouldNotPublishReplyEventForTopLevelComment() {
            CreateCommentRequest request = buildTopLevelRequest(1L);
            when(commentMapper.insert(any(CommentPO.class))).thenAnswer(inv -> {
                CommentPO po = inv.getArgument(0);
                po.setId(10L);
                return 1;
            });

            commentService.createComment(1L, request);

            verify(rabbitTemplate, never()).convertAndSend(
                    eq(NotificationConstants.EXCHANGE),
                    eq(NotificationConstants.ROUTING_KEY),
                    argThat((NotificationEvent e) ->
                            NotificationType.COMMENT_REPLIED.name().equals(e.getType())
                    )
            );
        }

        @Test
        @DisplayName("回复自己的评论不发布 REPLY 事件")
        void shouldNotPublishReplyEventWhenReplyingToSelf() {
            CreateCommentRequest request = buildReplyRequest(1L, 100L, 1L);
            when(commentMapper.insert(any(CommentPO.class))).thenAnswer(inv -> {
                CommentPO po = inv.getArgument(0);
                po.setId(10L);
                return 1;
            });

            commentService.createComment(1L, request);

            verify(rabbitTemplate, never()).convertAndSend(
                    eq(NotificationConstants.EXCHANGE),
                    eq(NotificationConstants.ROUTING_KEY),
                    argThat((NotificationEvent e) ->
                            NotificationType.COMMENT_REPLIED.name().equals(e.getType())
                    )
            );
        }

        @Test
        @DisplayName("MQ 发布失败不抛异常，不影响主流程")
        void shouldNotThrowOnMqFailure() {
            CreateCommentRequest request = buildReplyRequest(1L, 100L, 200L);
            when(commentMapper.insert(any(CommentPO.class))).thenAnswer(inv -> {
                CommentPO po = inv.getArgument(0);
                po.setId(10L);
                return 1;
            });
            doThrow(new RuntimeException("MQ down"))
                    .when(rabbitTemplate)
                    .convertAndSend(any(String.class), any(String.class), any(Object.class));

            commentService.createComment(1L, request);
        }
    }

    private CreateCommentRequest buildReplyRequest(Long userId, Long postId, Long replyToUserId) {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(postId.toString());
        req.setContent("回复测试内容");
        req.setParentId("100");
        req.setReplyToUserId(replyToUserId.toString());
        return req;
    }

    private CreateCommentRequest buildTopLevelRequest(Long postId) {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(postId.toString());
        req.setContent("一级评论");
        return req;
    }
}
