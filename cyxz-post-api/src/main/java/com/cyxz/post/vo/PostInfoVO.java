package com.cyxz.post.vo;

import lombok.Data;

/**
 * 帖子简要信息 VO
 * <p>用于内部 Feign 调用，仅返回评论服务需要的字段。
 */
@Data
public class PostInfoVO {

    /** 帖子 ID */
    private Long postId;

    /** 作者用户 ID */
    private Long userId;

    /** 帖子标题 */
    private String title;
}
