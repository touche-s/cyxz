package com.cyxz.user.dto;

import lombok.Data;

/**
 * 更新用户资料请求
 * <p>前端可选择性地传入需要修改的字段，null 字段不更新。
 */
@Data
public class UpdateProfileRequest {

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 性别：0=未知 1=男 2=女 */
    private Integer gender;

    /** 个人简介 */
    private String bio;
}
