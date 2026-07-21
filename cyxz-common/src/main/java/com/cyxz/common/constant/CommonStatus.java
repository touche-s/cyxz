package com.cyxz.common.constant;

/**
 * 通用状态常量
 */
public final class CommonStatus {

    private CommonStatus() {}

    /** 已删除 / 已取消 */
    public static final int DELETED = 0;

    /** 正常 / 已发布 / 已启用 */
    public static final int ACTIVE = 1;

    /** 草稿 */
    public static final int DRAFT = 0;

    /** 已发布 */
    public static final int PUBLISHED = 1;

    /** 用户已删除 */
    public static final int POST_DELETED = 2;
}
