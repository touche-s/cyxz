package com.cyxz.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 帖子计数变更事件，发到 RabbitMQ 由 cyxz-circle 消费
 * <p>用于事件驱动更新圈子 post_count，破除 circle→post 的 Feign 循环依赖。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCountEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** PUBLISH（+1）/ DELETE（-1） */
    private String action;

    private Long postId;
    private Long circleId;

    /** 事件唯一标识（UUID），用于消费端幂等去重 */
    private String eventId;
}
