package com.cyxz.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建评论请求
 */
@Data
public class CreateCommentRequest {

    /** 帖子 ID */
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论 ID（回复评论时传入，顶级评论不传） */
    private Long parentId;

    /** 被回复用户 ID（回复评论时传入） */
    private Long replyToUserId;
}
