package com.cyxz.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 * <p>会话列表展示用，包含对方用户信息和最后一条消息摘要。
 */
@Data
public class ConversationVO {

    private Long id;

    /** 对方用户 ID */
    private Long peerId;

    /** 对方昵称 */
    private String peerName;

    /** 对方头像 */
    private String peerAvatar;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后消息时间 */
    private LocalDateTime lastMessageAt;

    /** 当前用户在该会话的未读消息数 */
    private int unreadCount;
}
