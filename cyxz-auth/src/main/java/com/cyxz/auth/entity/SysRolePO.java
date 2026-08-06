package com.cyxz.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 * <p>对应 sys_role 表，scope 区分全局角色（GLOBAL）与圈子内角色（CIRCLE）。
 */
@Data
@TableName("sys_role")
public class SysRolePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色标识：SITE_OWNER / PLATFORM_ADMIN / USER / CIRCLE_OWNER / CIRCLE_ADMIN / CIRCLE_MEMBER */
    private String code;

    /** 显示名 */
    private String label;

    /** 作用域：GLOBAL 全局 / CIRCLE 圈子内 */
    private String scope;

    /** 描述 */
    private String description;

    /** 1=内置系统角色，不允许删除/改 code；0=业务可维护 */
    private Integer builtIn;

    /** 排序（小在前） */
    private Integer sort;
}
