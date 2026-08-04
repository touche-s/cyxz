package com.cyxz.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时将已有已通过帖子全量同步到 ES
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsFullSyncRunner implements ApplicationRunner {

    private final PostMapper postMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 启动时全量同步已通过帖子到 ES（失败仅记日志，不影响主流程）
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            List<PostPO> posts = postMapper.selectList(
                    new LambdaQueryWrapper<PostPO>()
                            .eq(PostPO::getStatus, 2)); // STATUS_APPROVED

            if (posts.isEmpty()) {
                log.info("ES 全量同步：无已通过帖子，跳过");
                return;
            }

            log.info("ES 全量同步开始：共 {} 条已通过帖子", posts.size());
            for (PostPO po : posts) {
                PostEsSyncEvent event = buildEvent(po);
                rabbitTemplate.convertAndSend(EsSyncConstants.EXCHANGE, EsSyncConstants.ROUTING_KEY, event);
            }
            log.info("ES 全量同步完成");
        } catch (Exception e) {
            log.error("ES 全量同步失败（不影响主流程）", e);
        }
    }

    private PostEsSyncEvent buildEvent(PostPO po) {
        return PostEsSyncEvent.builder()
                .action("CREATE")
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
