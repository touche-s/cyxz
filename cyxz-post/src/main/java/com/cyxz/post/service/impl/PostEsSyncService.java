package com.cyxz.post.service.impl;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.constant.PostCountConstants;
import com.cyxz.common.event.PostCountEvent;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.entity.PostPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 帖子 ES 索引同步服务
 * <p>将帖子变更通过 MQ 事件通知 cyxz-search 服务同步 ES 索引。
 * <p>MQ 发送失败时写入 Redis 失败队列，由 {@link #retryFailedSync()} 定时重试，
 * 避免 DB 与 ES 永久不一致（生产端发送失败的消息不会进入 MQ，DLQ 兜底不到）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostEsSyncService {

    /** Redis 失败队列 key：List 结构，LPUSH 入队、RPOP 出队 */
    private static final String FAILED_QUEUE_KEY = "post:es:sync:failed";
    private static final String FAILED_COUNT_QUEUE_KEY = "post:count:sync:failed";

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 同步帖子到 ES：APPROVED 状态写入，其他状态删除
     */
    public void syncPostToEs(PostPO po) {
        String action = po.getStatus() != null && po.getStatus() == PostStatus.APPROVED ? "CREATE" : "DELETE";
        PostEsSyncEvent event = buildSyncEvent(po, action);
        try {
            rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("ES 同步消息发送失败，已入失败队列等待重试: postId={}", po.getId(), e);
            enqueueFailedSync(event);
        }
    }

    public void syncPostToEsDelete(Long postId) {
        try {
            PostEsSyncEvent event = PostEsSyncEvent.builder()
                    .action("DELETE")
                    .postId(postId)
                    .build();
            rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("ES 同步删除消息发送失败，已入失败队列等待重试: postId={}", postId, e);
            PostEsSyncEvent event = PostEsSyncEvent.builder()
                    .action("DELETE")
                    .postId(postId)
                    .build();
            enqueueFailedSync(event);
        }
    }

    /**
     * 定时重试失败队列中的 ES 同步消息
     * <p>每 30 秒扫描一次，单次最多处理 50 条，避免任务积压时占用过多线程。
     * 重试仍失败的留在队列里下次再试。
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 60_000L)
    public void retryFailedSync() {
        int maxRetry = 50;
        int succeeded = 0;
        int failed = 0;
        for (int i = 0; i < maxRetry; i++) {
            String json = stringRedisTemplate.opsForList().rightPop(CacheKeyConstants.POST_ES_SYNC_FAILED_QUEUE);
            if (json == null) {
                break;
            }
            try {
                // 简单重发：直接把 JSON 字符串作为消息体发出（消费端按 JSON 解析）
                rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, json);
                succeeded++;
            } catch (Exception e) {
                // 重试失败：放回队首，留待下次
                stringRedisTemplate.opsForList().leftPush(CacheKeyConstants.POST_ES_SYNC_FAILED_QUEUE, json);
                failed++;
                log.warn("ES 同步重试失败，已放回队列: {}", json, e);
                break;
            }
        }
        if (succeeded > 0 || failed > 0) {
            log.info("ES 同步失败队列重试完成: succeeded={}, failed={}", succeeded, failed);
        }
    }

    private void enqueueFailedSync(PostEsSyncEvent event) {
        try {
            stringRedisTemplate.opsForList().leftPush(FAILED_QUEUE_KEY, toJson(event));
        } catch (Exception redisEx) {
            // Redis 也不可用时只能记日志，等下次启动 EsFullSyncRunner 全量同步兜底
            log.error("写入 ES 同步失败队列失败，依赖全量同步兜底: postId={}", event.getPostId(), redisEx);
        }
    }

    private String toJson(Object event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (Exception e) {
            log.error("事件序列化失败: {}", event.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    private PostEsSyncEvent buildSyncEvent(PostPO po, String action) {
        return PostEsSyncEvent.builder()
                .action(action)
                .postId(po.getId())
                .userId(po.getUserId())
                .circleId(po.getCircleId())
                .sectionId(po.getSectionId())
                .postType(po.getPostType())
                .title(po.getTitle())
                .content(po.getContent())
                .cover(po.getCover())
                .tags(po.getTags())
                .status(po.getStatus())
                .likes(po.getLikes())
                .comments(po.getComments())
                .views(po.getViews())
                .collections(po.getCollections())
                .createTime(po.getCreateTime() != null
                        ? po.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : System.currentTimeMillis())
                .build();
    }

    /**
     * 发送帖子计数变更事件，由 cyxz-circle 消费更新 post_count
     *
     * @param po     帖子实体（需有 id 和 circleId）
     * @param action {@link PostCountConstants#ACTION_PUBLISH} 或 {@link PostCountConstants#ACTION_DELETE}
     */
    public void publishCountEvent(PostPO po, String action) {
        if (po.getCircleId() == null) {
            return;
        }
        PostCountEvent event = PostCountEvent.builder()
                .action(action)
                .postId(po.getId())
                .circleId(po.getCircleId())
                .eventId(UUID.randomUUID().toString())
                .build();
        try {
            rabbitTemplate.convertAndSend(PostCountConstants.EXCHANGE, PostCountConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("发送帖子计数事件失败，已入失败队列等待重试: postId={}, circleId={}, action={}",
                    po.getId(), po.getCircleId(), action, e);
            enqueueFailedCountSync(event);
        }
    }

    /**
     * 定时重试失败队列中的计数同步消息
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 90_000L)
    public void retryFailedCountSync() {
        int maxRetry = 50;
        int succeeded = 0;
        int failed = 0;
        for (int i = 0; i < maxRetry; i++) {
            String json = stringRedisTemplate.opsForList().rightPop(CacheKeyConstants.POST_COUNT_SYNC_FAILED_QUEUE);
            if (json == null) {
                break;
            }
            try {
                rabbitTemplate.convertAndSend(PostCountConstants.EXCHANGE, PostCountConstants.ROUTING_KEY, json);
                succeeded++;
            } catch (Exception e) {
                stringRedisTemplate.opsForList().leftPush(FAILED_COUNT_QUEUE_KEY, json);
                failed++;
                log.warn("帖子计数同步重试失败，已放回队列: {}", json, e);
                break;
            }
        }
        if (succeeded > 0 || failed > 0) {
            log.info("帖子计数同步失败队列重试完成: succeeded={}, failed={}", succeeded, failed);
        }
    }

    private void enqueueFailedCountSync(PostCountEvent event) {
        try {
            stringRedisTemplate.opsForList().leftPush(CacheKeyConstants.POST_COUNT_SYNC_FAILED_QUEUE, toJson(event));
        } catch (Exception redisEx) {
            log.error("写入计数同步失败队列失败，计数可能丢失: postId={}, circleId={}",
                    event.getPostId(), event.getCircleId(), redisEx);
        }
    }
}
