package com.cyxz.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建通知请求 DTO
 * <p>用于内部 Feign 调用创建消息通知。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    /** 接收者用户 ID */
    private Long receiverId;

    /** 发送者用户 ID（触发通知的人） */
    private Long senderId;

    /** 通知类型 */
    private String type;

    /** 目标 ID（帖子 ID / 评论 ID） */
    private Long targetId;

    /** 目标类型（post / comment） */
    private String targetType;

    /** 关联 ID（如评论所属的帖子 ID） */
    private Long relatedId;

    /** 通知附带的内容摘要 */
    private String content;
}
