package com.cyxz.governance.api.constant;

import com.cyxz.common.constant.CacheKeyConstants;

/**
 * 治理中心常量
 * <p>集中定义 RabbitMQ 交换机/队列/死信、举报目标类型与状态。
 */
public final class GovernanceConstants {

    private GovernanceConstants() {}

    // ===== RabbitMQ（治理事件总线，Topic Exchange + routing key 区分事件类型） =====
    public static final String EXCHANGE = "cyxz.governance.exchange";
    public static final String DLX = "cyxz.governance.dlx";

    // --- 内容处置事件（举报通过 → 删内容） ---
    public static final String ROUTING_KEY = "governance.takedown";
    public static final String QUEUE = "cyxz.governance.takedown.queue";
    public static final String DLQ = "cyxz.governance.takedown.dlq.queue";
    public static final String DEAD_ROUTING_KEY = "governance.takedown.dead";

    // ===== 举报目标类型 =====
    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";

    // ===== 举报状态 =====
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    /** 内容处置事件发送失败时的 Redis 补偿队列（List 结构） */
    public static final String TAKEDOWN_FAILED_QUEUE_KEY = CacheKeyConstants.GOVERNANCE_TAKEDOWN_FAILED_QUEUE;
}
