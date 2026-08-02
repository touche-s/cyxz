package com.cyxz.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 通知实体
 * <p>对应 notification 表，存储用户消息通知记录，支持点赞、评论、回复、收藏、关注等类型。
 */
@Data
@TableName("notification")
public class NotificationPO extends BaseEntity {

    /** 主键 */
    @TableId
    private Long id;

    /** 接收者用户 ID */
    private Long receiverId;

    /** 发送者用户 ID */
    private Long senderId;

    /** 通知类型 */
    private String type;

    /** 目标 ID（帖子 / 评论 ID） */
    private Long targetId;

    /** 目标类型（post / comment） */
    private String targetType;

    /** 关联 ID（如评论所属帖子 ID） */
    private Long relatedId;

    /** 通知附带的内容摘要（最长 200 字） */
    private String content;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;
}
