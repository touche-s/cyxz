package com.cyxz.auth.feign.dto;

import lombok.Data;

/**
 * 圈子角色分配/撤销请求
 */
@Data
public class CircleRoleRequest {

    /** 目标用户 ID */
    private Long userId;

    /** 圈子角色 ID（sys_role.id，见 CircleRoleConstants） */
    private Long roleId;

    /** 圈子 ID */
    private Long circleId;
}
