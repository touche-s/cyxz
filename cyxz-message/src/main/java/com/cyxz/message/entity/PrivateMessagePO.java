package com.cyxz.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 私信消息实体
 * <p>对应 private_message 表，存储每条私信的详细内容。
 */
@Data
@TableName("private_message")
public class PrivateMessagePO extends BaseEntity {

    @TableId
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private String content;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;
}
