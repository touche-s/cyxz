package com.cyxz.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 举报记录实体，对应 report 表
 * <p>同一用户对同一对象仅可举报一次（uk_reporter_target 唯一索引约束）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report")
public class ReportPO extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
}
