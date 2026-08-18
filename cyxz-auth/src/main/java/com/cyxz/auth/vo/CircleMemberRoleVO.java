package com.cyxz.auth.vo;

import lombok.Data;

/**
 * 圈子成员角色视图对象（auth 侧）
 * <p>仅含角色信息（不含昵称/头像，后者由 user 服务提供，circle 侧合并）。
 */
@Data
public class CircleMemberRoleVO {

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
