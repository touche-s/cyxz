package com.cyxz.audit.api.constant;

/**
 * 审计中心常量
 * <p>集中定义 RabbitMQ 交换机/队列/死信，以及各业务服务发布的审计动作枚举。
 */
public final class AuditConstants {

    private AuditConstants() {}

    // ===== RabbitMQ（审计事件总线，Topic Exchange + routing key） =====
    public static final String EXCHANGE = "cyxz.audit.exchange";
    public static final String ROUTING_KEY = "audit.log";
    public static final String QUEUE = "cyxz.audit.queue";
    public static final String DLX = "cyxz.audit.dlx";
    public static final String DLQ = "cyxz.audit.dlq";
    public static final String DEAD_ROUTING_KEY = "audit.dead";

    // ===== 审计动作 =====
    /** 用户封禁 */
    public static final String ACTION_USER_DISABLE = "USER_DISABLE";
    /** 用户解禁 */
    public static final String ACTION_USER_ENABLE = "USER_ENABLE";
    /** 帖子删除 */
    public static final String ACTION_POST_DELETE = "POST_DELETE";
    /** 帖子审核通过 */
    public static final String ACTION_POST_APPROVE = "POST_APPROVE";
    /** 帖子审核驳回 */
    public static final String ACTION_POST_REJECT = "POST_REJECT";
    /** 举报通过 */
    public static final String ACTION_REPORT_APPROVE = "REPORT_APPROVE";
    /** 举报驳回 */
    public static final String ACTION_REPORT_REJECT = "REPORT_REJECT";
    /** 圈子创建审批通过 */
    public static final String ACTION_CIRCLE_APPROVE = "CIRCLE_APPROVE";
    /** 圈子创建审批驳回 */
    public static final String ACTION_CIRCLE_REJECT = "CIRCLE_REJECT";
    /** 圈子加入申请通过 */
    public static final String ACTION_CIRCLE_JOIN_APPROVE = "CIRCLE_JOIN_APPROVE";
    /** 圈子加入申请驳回 */
    public static final String ACTION_CIRCLE_JOIN_REJECT = "CIRCLE_JOIN_REJECT";
}
