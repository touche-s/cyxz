package com.cyxz.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体
 * <p>对应 sys_permission 表，统一一套权限码，不区分全局/圈子。
 */
@Data
@TableName("sys_permission")
public class SysPermissionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 权限标识：post:review:approve */
    private String code;

    /** 显示名 */
    private String label;

    /** 资源：post/user/circle/upload */
    private String resource;

    /** 操作 */
    private String action;

    private LocalDateTime createTime;
}
