package com.cyxz.circle.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 圈子创建申请 VO
 */
@Data
public class CircleApplicationVO {

    /** 申请记录 ID */
    private Long id;

    /** 申请人用户 ID（通过后成为圈主） */
    private Long applicantId;

    /** 圈子名称 */
    private String name;

    /** 圈子简介 */
    private String intro;

    /** 圈子头像 URL */
    private String avatar;

    /** 圈子封面 URL */
    private String cover;

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
