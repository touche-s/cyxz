package com.cyxz.circle.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入圈申请 VO
 */
@Data
public class CircleJoinApplicationVO {

    /** 申请记录 ID */
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

    /** 申请创建时间 */
    private LocalDateTime createTime;
}
