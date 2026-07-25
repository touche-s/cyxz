package com.cyxz.message.api.enums;

import lombok.Getter;

/**
 * 通知类型枚举
 * <p>定义系统支持的五种通知类型及其对应的前端展示文案和归类标识。
 */
@Getter
public enum NotificationType {

    POST_LIKED("赞了你的帖子", "like"),
    POST_COMMENTED("评论了你的帖子", "comment"),
    COMMENT_REPLIED("回复了你的评论", "reply"),
    POST_COLLECTED("收藏了你的帖子", "collect"),
    USER_FOLLOWED("关注了你", "follow");

    /** 动作文案（前端展示用） */
    private final String actionText;

    /** 前端归类标识 */
    private final String frontType;

    NotificationType(String actionText, String frontType) {
        this.actionText = actionText;
        this.frontType = frontType;
    }
}
