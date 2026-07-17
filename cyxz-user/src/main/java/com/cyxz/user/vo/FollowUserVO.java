package com.cyxz.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 粉丝/关注列表用户视图对象
 * <p>在 UserProfileVO 基础上增加 following 字段，用于标识当前用户是否已关注对方。
 */
@Data
public class FollowUserVO {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 关注时间 */
    private LocalDateTime createTime;

    /** 当前用户是否已关注对方（用于粉丝列表"回关"按钮） */
    private Boolean following;
}
