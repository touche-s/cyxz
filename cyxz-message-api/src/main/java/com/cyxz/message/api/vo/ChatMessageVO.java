package com.cyxz.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息视图对象
 */
@Data
public class ChatMessageVO {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private String content;

    private boolean read;

    private LocalDateTime createTime;
}
