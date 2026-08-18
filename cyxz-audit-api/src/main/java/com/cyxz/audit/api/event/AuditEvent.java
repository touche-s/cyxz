package com.cyxz.audit.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志事件
 * <p>各业务服务在执行关键操作（用户封禁/解禁、帖子删除/审核、举报处理、圈子审批等）后发布，
 * 由审计中心消费并落库，实现操作行为的统一留痕与可追溯，避免各服务直写审计库。
 *
 * <pre>{@code
 * 业务服务执行关键操作
 *      ↓ 发布 AuditEvent
 * audit 服务消费 → 写入 audit_log 表
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作动作（对应 {@link com.cyxz.audit.api.constant.AuditConstants} 中的 ACTION_* 常量） */
    private String action;

    /** 操作对象类型（如 USER / POST / REPORT / CIRCLE 等） */
    private String targetType;

    /** 操作对象 ID */
    private Long targetId;

    /** 操作详情（补充说明） */
    private String detail;

    /** 操作来源 IP */
    private String ip;

    /** 操作时间 */
    private LocalDateTime createTime;

    /** 事件唯一标识（UUID），用于消费端幂等去重 */
    private String eventId;
}
