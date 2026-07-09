package com.cyxz.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料实体
 * <p>对应 user_profile 表，与 sys_user 一一对应，存储昵称、头像等展示信息。
 */
@Data
@TableName("user_profile")
public class UserProfilePO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
