package com.cyxz.message.constant;

/**
 * 通知 MQ 常量，统一管理 exchange、routingKey，避免各模块散落魔数字符串
 */
public final class NotificationConstants {

    private NotificationConstants() {}

    /** 通知交换机 */
    public static final String EXCHANGE = "cyxz.notification.exchange";

    /** 通知创建路由键 */
    public static final String ROUTING_KEY = "notification.create";

    /** 死信交换机 */
    public static final String DLX = "cyxz.notification.dlx";

    /** 死信队列 */
    public static final String DLQ = "cyxz.notification.dlq";

    /** 死信路由键 */
    public static final String DEAD_ROUTING_KEY = "notification.dead";

    /** 通知队列 */
    public static final String QUEUE = "cyxz.notification.queue";
}
