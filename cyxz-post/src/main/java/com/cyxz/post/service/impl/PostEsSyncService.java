package com.cyxz.post.service.impl;

import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.entity.PostPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 帖子 ES 索引同步服务
 * <p>将帖子变更通过 MQ 事件通知 cyxz-search 服务同步 ES 索引。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostEsSyncService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 同步帖子到 ES：APPROVED 状态写入，其他状态删除
     */
    public void syncPostToEs(PostPO po) {
        try {
            String action = po.getStatus() != null && po.getStatus() == PostStatus.APPROVED ? "CREATE" : "DELETE";
            PostEsSyncEvent event = buildSyncEvent(po, action);
            rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("ES 同步消息发送失败: postId={}", po.getId(), e);
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
            log.error("ES 同步删除消息发送失败: postId={}", postId, e);
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
}
