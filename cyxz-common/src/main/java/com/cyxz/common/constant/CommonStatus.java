package com.cyxz.common.constant;

/**
 * 通用状态常量
 * <p>仅用于关系表 / 简单开关表的二态字段（如 post_like、post_collect、user_follow、comment 等）。
 * <p>帖子生命周期状态请使用 {@link PostStatus}。
 */
public final class CommonStatus {

    private CommonStatus() {}

    /** 已删除 / 已取消 */
    public static final int DELETED = 0;

    /** 正常 / 有效 / 已启用 */
    public static final int ACTIVE = 1;
}
