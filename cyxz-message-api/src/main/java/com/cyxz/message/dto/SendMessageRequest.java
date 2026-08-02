package com.cyxz.message.dto;

import lombok.Data;

/**
 * 发送私信请求
 */
@Data
public class SendMessageRequest {

    /** 接收者用户 ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;
}
