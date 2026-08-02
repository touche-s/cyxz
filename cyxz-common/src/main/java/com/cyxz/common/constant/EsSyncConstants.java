package com.cyxz.common.constant;

/**
 * ES 索引同步 MQ 常量
 */
public final class EsSyncConstants {

    private EsSyncConstants() {}

    public static final String EXCHANGE = "cyxz.post.exchange";
    public static final String ROUTING_KEY = "post.es.sync";
    public static final String QUEUE = "cyxz.post.es.sync.queue";

    /** 死信交换机/队列：ES 同步失败的消息转入死信，避免丢失 */
    public static final String DLX = "cyxz.post.es.dlx";
    public static final String DLQ = "cyxz.post.es.dlq.queue";
    public static final String DEAD_ROUTING_KEY = "post.es.dead";
}
