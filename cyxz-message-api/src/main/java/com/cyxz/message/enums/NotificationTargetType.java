package com.cyxz.message.enums;

/**
 * 通知目标类型常量
 * <p>NotificationEvent.targetType 的取值集合，标识通知关联的实体类型。
 * 散落在各服务的 builder 中硬编码，集中此处避免不一致。
 */
public final class NotificationTargetType {

    private NotificationTargetType() {}

    /** 目标为帖子 */
    public static final String POST = "post";

    /** 目标为评论 */
    public static final String COMMENT = "comment";

    /** 目标为用户 */
    public static final String USER = "user";
}
