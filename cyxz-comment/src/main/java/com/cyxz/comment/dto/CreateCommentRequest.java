package com.cyxz.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求
 */
@Data
public class CreateCommentRequest {

    /** 帖子 ID（前端传 String 避免 JS 精度丢失） */
    @NotBlank(message = "帖子ID不能为空")
    private String postId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最长500字")
    private String content;

    /** 父评论 ID（回复评论时传入，顶级评论不传） */
    private String parentId;

    /** 被回复用户 ID（回复评论时传入） */
    private String replyToUserId;

    // ==== 内部解析后的 Long 值 ====

    public Long getPostIdAsLong() {
        return parseLong(postId);
    }

    public Long getParentIdAsLong() {
        return parseLong(parentId);
    }

    public Long getReplyToUserIdAsLong() {
        return parseLong(replyToUserId);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
