package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 入圈申请实体，对应 circle_join_application 表
 * <p>用户申请加入圈子 → 管理员/圈主审核 → 通过后直接调用 joinCircle 加入成员。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("circle_join_application")
public class CircleJoinApplicationPO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户 ID */
    private Long applicantId;

    /** 要加入的圈子 ID */
    private Long circleId;

    /** 申请理由 */
    private String reason;

    /** 状态：PENDING / APPROVED / REJECTED */
    private String status;

    /** 审核人用户 ID */
    private Long reviewerId;

    /** 审核意见 */
    private String reviewNote;

    /** 审核时间 */
    private LocalDateTime reviewedAt;
}
