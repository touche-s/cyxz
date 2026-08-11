package com.cyxz.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志 VO（管理端列表）
 */
@Data
public class AuditLogVO {

    /** 主键 ID */
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

    /** 创建时间 */
    private LocalDateTime createTime;
}
