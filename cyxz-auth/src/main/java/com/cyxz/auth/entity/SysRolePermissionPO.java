package com.cyxz.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关联实体
 * <p>对应 sys_role_permission 表，复合主键 (role_id, permission_id)。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermissionPO {

    private Long roleId;

    private Long permissionId;
}
