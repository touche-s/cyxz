package com.cyxz.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话实体
 * <p>对应 conversation 表，记录两个用户之间的对话关系。
 * <p>约定 user_id_1 < user_id_2，避免 A↔B 产生两条会话。
 */
@Data
@TableName("conversation")
public class ConversationPO extends BaseEntity {

    @TableId
    private Long id;

    /** 用户 ID（较小值） */
    private Long userId1;

    /** 用户 ID（较大值） */
    private Long userId2;

    /** 最后一条消息内容（冗余，会话列表预览用） */
    private String lastMessage;

    /** 最后消息时间（排序用） */
    private LocalDateTime lastMessageAt;

    /** 用户1的未读消息数 */
    private Integer unreadCount1;

    /** 用户2的未读消息数 */
    private Integer unreadCount2;
}
