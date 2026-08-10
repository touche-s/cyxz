package com.cyxz.common.constant;

/**
 * 帖子计数变更 MQ 常量
 * <p>cyxz-post 发送，cyxz-circle 消费，用于事件驱动更新圈子帖子数。
 */
public final class PostCountConstants {

    private PostCountConstants() {}

    public static final String EXCHANGE = "cyxz.post.count.exchange";
    public static final String ROUTING_KEY = "post.count.change";
    public static final String QUEUE = "cyxz.post.count.queue";

    /** 死信交换机/队列 */
    public static final String DLX = "cyxz.post.count.dlx";
    public static final String DLQ = "cyxz.post.count.dlq.queue";
    public static final String DEAD_ROUTING_KEY = "post.count.dead";

    /** 事件动作 */
    public static final String ACTION_PUBLISH = "PUBLISH";
    public static final String ACTION_DELETE = "DELETE";
}
