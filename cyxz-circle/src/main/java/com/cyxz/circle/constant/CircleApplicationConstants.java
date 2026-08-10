package com.cyxz.circle.constant;

/**
 * 圈子申请常量
 * <p>建圈/入圈申请的通用状态定义，独立于 governance 模块。
 */
public final class CircleApplicationConstants {

    private CircleApplicationConstants() {}

    /** 待审核 */
    public static final String STATUS_PENDING = "PENDING";

    /** 已通过 */
    public static final String STATUS_APPROVED = "APPROVED";

    /** 已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
}
