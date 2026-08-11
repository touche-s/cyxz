package com.cyxz.governance.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报记录 VO（管理端列表/详情）
 */
@Data
public class ReportVO {

    private Long id;

    /** 举报人用户 ID */
    private Long reporterId;

    /** 举报对象类型：POST / COMMENT */
    private String targetType;

    /** 举报对象 ID */
    private Long targetId;

    /** 举报原因 */
    private String reason;

    /** 状态：PENDING / APPROVED / REJECTED */
    private String status;

    /** 处理人（管理员）用户 ID */
    private Long handlerId;

    /** 处理意见 */
    private String handlerNote;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 创建时间 */
    private LocalDateTime createTime;
}
