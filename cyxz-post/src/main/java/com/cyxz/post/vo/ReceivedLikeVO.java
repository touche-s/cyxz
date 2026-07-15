package com.cyxz.post.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收到的点赞视图对象
 */
@Data
public class ReceivedLikeVO {

    /** 点赞记录 ID */
    private Long likeId;

    /** 帖子 ID */
    private Long postId;

    /** 帖子标题 */
    private String postTitle;

    /** 点赞用户 ID */
    private Long userId;

    /** 点赞用户昵称（由 Feign 批量查询填充） */
    private String userName;

    /** 点赞用户头像（由 Feign 批量查询填充） */
    private String userAvatar;

    /** 点赞时间 */
    private LocalDateTime createTime;
}
