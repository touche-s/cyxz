package com.cyxz.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 VO
 * <p>用于前端展示通知消息，含发送者信息和目标对象摘要。
 */
@Data
public class NotificationVO {

    /** 通知 ID */
    private Long id;

    /** 发送者用户 ID */
    private Long senderId;

    /** 发送者昵称 */
    private String senderName;

    /** 发送者头像 */
    private String senderAvatar;

    /** 通知类型 */
    private String type;

    /** 动作文案（如"赞了你的帖子"） */
    private String actionText;

    /** 目标 ID（帖子/评论 ID） */
    private Long targetId;

    /** 目标类型（post / comment） */
    private String targetType;

    /** 目标标题（帖子标题 / 评论内容摘要） */
    private String targetTitle;

    /** 通知附带的内容摘要 */
    private String content;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
