package com.cyxz.message.constant;

/**
 * 消息域常量
 * <p>统一管理通知/私信的 MQ 交换机、路由键与已读状态取值，避免各模块散落魔数字符串。
 */
public final class NotificationConstants {

    private NotificationConstants() {}

    /** 未读 */
    public static final int UNREAD = 0;

    /** 已读 */
    public static final int READ = 1;

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
