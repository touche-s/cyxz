package com.cyxz.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户资料实体
 * <p>对应 user_profile 表，与 sys_user 一一对应，存储昵称、头像等展示信息。
 */
@Data
@TableName("user_profile")
public class UserProfilePO extends BaseEntity {

    /** 用户 ID（关联 sys_user.id） */
    @TableId
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 性别：0=未知 1=男 2=女 */
    private Integer gender;

    /** 个人简介 */
    private String bio;

    /** 生日 */
    private LocalDate birthday;
}
