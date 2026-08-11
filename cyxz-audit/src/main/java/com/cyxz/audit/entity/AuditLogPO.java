package com.cyxz.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志实体，对应 audit_log 表
 * <p>由审计中心消费 {@code AuditEvent} 异步落库，记录平台关键操作行为。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLogPO extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作动作 */
    private String action;

    /** 操作对象类型 */
    private String targetType;

    /** 操作对象 ID */
    private Long targetId;

    /** 操作详情 */
    private String detail;

    /** 操作来源 IP */
    private String ip;
}
