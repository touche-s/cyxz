package com.cyxz.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信消息实体
 * <p>对应 private_message 表，存储每条私信的详细内容。
 */
@Data
@TableName("private_message")
public class PrivateMessagePO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private String content;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
