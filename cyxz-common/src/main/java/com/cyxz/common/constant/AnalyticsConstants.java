package com.cyxz.common.constant;

/**
 * 数据统计常量
 * <p>集中定义统计事件 RabbitMQ 交换机/队列/死信及业务指标类型。
 */
public final class AnalyticsConstants {

    private AnalyticsConstants() {}

    // ===== RabbitMQ（统计事件总线，Topic Exchange + 固定 routing key） =====
    public static final String EXCHANGE = "cyxz.analytics.exchange";
    public static final String DLX = "cyxz.analytics.dlx";

    // --- 统计指标事件（业务指标增量 → analytics 聚合） ---
    public static final String ROUTING_KEY = "analytics.metric";
    public static final String QUEUE = "cyxz.analytics.queue";
    public static final String DLQ = "cyxz.analytics.dlq";
    public static final String DEAD_ROUTING_KEY = "analytics.dead";

    // ===== 统计指标类型 =====
    /** 新增用户数 */
    public static final String METRIC_NEW_USER = "NEW_USER";
    /** 新增帖子数 */
    public static final String METRIC_NEW_POST = "NEW_POST";
    /** 帖子审核通过数 */
    public static final String METRIC_POST_APPROVED = "POST_APPROVED";
    /** 帖子审核驳回数 */
    public static final String METRIC_POST_REJECTED = "POST_REJECTED";
    /** 新增圈子数 */
    public static final String METRIC_NEW_CIRCLE = "NEW_CIRCLE";
    /** 新增加入圈子数 */
    public static final String METRIC_NEW_JOIN = "NEW_JOIN";
    /** 举报处理数 */
    public static final String METRIC_REPORT_HANDLED = "REPORT_HANDLED";
}
