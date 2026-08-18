package com.cyxz.auth.feign.vo;

import lombok.Data;

/**
 * 圈子成员角色视图对象
 * <p>由 auth 服务提供成员在圈子内的角色信息（角色来自 sys_user_role + sys_role），
 * 昵称/头像等用户资料由 user 服务提供（circle 侧合并）。
 */
@Data
public class CircleMemberVO {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色 ID */
    private Long roleId;

    /** 角色 code：CIRCLE_OWNER / CIRCLE_ADMIN / CIRCLE_MEMBER */
    private String roleCode;

    /** 角色中文名称：圈主 / 圈子管理员 / 圈子成员 */
    private String roleLabel;

    /** 加入时间（yyyy-MM-dd HH:mm） */
    private String joinTime;
}
