package com.cyxz.comment.dto;

import lombok.Data;

/**
 * 创建评论请求
 */
@Data
public class CreateCommentRequest {

    /** 帖子 ID */
    private Long postId;

    /** 评论内容 */
    private String content;

    /** 父评论 ID（回复评论时传入，顶级评论不传） */
    private Long parentId;

    /** 被回复用户 ID（回复评论时传入） */
    private Long replyToUserId;
}
