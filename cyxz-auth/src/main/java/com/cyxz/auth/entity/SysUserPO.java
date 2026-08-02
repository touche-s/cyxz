package com.cyxz.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 系统用户实体
 * <p>对应 sys_user 表，Auth 服务仅维护认证相关字段，
 * 用户资料（昵称、头像等）由 cyxz-user 服务管理。
 */
@Data
@TableName("sys_user")
public class SysUserPO extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 状态：1-正常 0-禁用 */
    private Integer status;

    /** 角色：admin / user */
    private String role;

    @TableLogic
    private Integer deleted;
}
