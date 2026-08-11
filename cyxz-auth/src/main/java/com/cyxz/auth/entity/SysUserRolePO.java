package com.cyxz.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联实体
 * <p>对应 sys_user_role 表，circle_id=0 表示全局角色，非 0 表示圈子内角色。
 */
@Data
@TableName("sys_user_role")
public class SysUserRolePO {

    private Long userId;

    private Long roleId;

    /** 0=全局角色，非 0=圈子内角色 */
    private Long circleId;

    private LocalDateTime createTime;
}
