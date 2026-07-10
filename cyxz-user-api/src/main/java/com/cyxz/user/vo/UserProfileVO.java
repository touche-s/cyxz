package com.cyxz.user.vo;

import lombok.Data;

/**
 * 用户资料视图对象
 * <p>用于前端展示，birthday 已格式化为 yyyy-MM-dd 字符串。
 */
@Data
public class UserProfileVO {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 性别：0=未知 1=男 2=女 */
    private Integer gender;

    /** 个人简介 */
    private String bio;

    /** 生日（yyyy-MM-dd） */
    private String birthday;
}
