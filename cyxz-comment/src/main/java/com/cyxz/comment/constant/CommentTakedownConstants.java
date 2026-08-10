package com.cyxz.comment.constant;

import com.cyxz.governance.api.constant.GovernanceConstants;

/**
 * 评论处置队列常量
 * <p>消费端独立声明队列，与治理中心的 {@value GovernanceConstants#EXCHANGE}
 * 共用同一个 Topic Exchange，通过 {@value GovernanceConstants#ROUTING_KEY} 路由分发。
 */
public final class CommentTakedownConstants {

    private CommentTakedownConstants() {}

    /** 评论处置队列（消费端独立队列，绑定治理中心 Exchange） */
    public static final String QUEUE = "cyxz.governance.takedown.comment.queue";

    /** 评论处置死信队列 */
    public static final String DLQ = "cyxz.governance.takedown.comment.dlq";

    /** 死信 routing key */
    public static final String DEAD_ROUTING_KEY = "governance.takedown.comment.dead";
}
