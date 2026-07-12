package com.cyxz.comment.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 */
@Data
public class CommentVO {

    /** 评论 ID */
    private Long id;

    /** 帖子 ID */
    private Long postId;

    /** 评论用户 ID */
    private Long userId;

    /** 评论用户昵称 */
    private String userName;

    /** 评论用户头像 */
    private String userAvatar;

    /** 评论内容 */
    private String content;

    /** 父评论 ID（null 表示顶级评论） */
    private Long parentId;

    /** 被回复用户 ID */
    private Long replyToUserId;

    /** 被回复用户昵称 */
    private String replyToUserName;

    /** 点赞数 */
    private Integer likes;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 子评论列表（仅顶级评论包含） */
    private List<CommentVO> children;

    /** 创建时间 */
    private LocalDateTime createTime;
}
