package com.cyxz.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私信请求
 */
@Data
public class SendMessageRequest {

    /** 接收者用户 ID */
    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容最长500字")
    private String content;
}
