package com.cyxz.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 评论实体
 * <p>对应 comment 表，存储评论核心信息。
 */
@Data
@TableName("comment")
public class CommentPO extends BaseEntity {

    /** 评论 ID（雪花算法） */
    @TableId
    private Long id;

    /** 帖子 ID */
    private Long postId;

    /** 评论用户 ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 父评论 ID（回复时使用，null 表示顶级评论） */
    private Long parentId;

    /** 被回复用户 ID */
    private Long replyToUserId;

    /** 帖子作者 ID（冗余字段，用于查询用户收到的评论） */
    private Long postAuthorId;

    /** 点赞数 */
    private Integer likes;

    /** 状态：0=已删除 1=正常 */
    private Integer status;
}
