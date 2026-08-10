package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 圈子创建申请实体，对应 circle_application 表
 * <p>用户申请创建圈子 → 管理员审核 → 通过后直接调用 createCircle 建圈。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("circle_application")
public class CircleApplicationPO extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
}
