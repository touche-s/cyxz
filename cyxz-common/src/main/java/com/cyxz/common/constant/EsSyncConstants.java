package com.cyxz.common.constant;

/**
 * ES 索引同步 MQ 常量
 */
public final class EsSyncConstants {

    private EsSyncConstants() {}

    public static final String EXCHANGE = "cyxz.post.exchange";
    public static final String ROUTING_KEY = "post.es.sync";
    public static final String QUEUE = "cyxz.post.es.sync.queue";
}
