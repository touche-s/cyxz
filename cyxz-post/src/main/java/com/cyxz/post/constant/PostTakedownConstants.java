package com.cyxz.post.constant;

import com.cyxz.governance.api.constant.GovernanceConstants;

/**
 * 帖子处置队列常量
 * <p>消费端独立声明队列，与治理中心的 {@value GovernanceConstants#EXCHANGE}
 * 共用同一个 Topic Exchange，通过 {@value GovernanceConstants#ROUTING_KEY} 路由分发。
 */
public final class PostTakedownConstants {

    private PostTakedownConstants() {}

    /** 帖子处置队列（消费端独立队列，绑定治理中心 Exchange） */
    public static final String QUEUE = "cyxz.governance.takedown.post.queue";

    /** 帖子处置死信队列 */
    public static final String DLQ = "cyxz.governance.takedown.post.dlq";

    /** 死信 routing key */
    public static final String DEAD_ROUTING_KEY = "governance.takedown.post.dead";
}
