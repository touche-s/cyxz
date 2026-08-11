package com.cyxz.circle.constant;

/**
 * 圈子角色 ID 常量
 * <p>对应 sys_role 表中 built_in=1 的圈子内角色，ID 由初始化 SQL 固定，不可变更。
 * 业务层授权以 sys_user_role 关联表为准，本常量仅用于分配/撤销角色时定位 role_id。
 */
public final class CircleRoleConstants {

    /** 圈主角色 ID（sys_role.id=4，CIRCLE_OWNER） */
    public static final long CIRCLE_OWNER_ROLE_ID = 4L;
    /** 圈子管理员角色 ID（sys_role.id=5，CIRCLE_ADMIN） */
    public static final long CIRCLE_ADMIN_ROLE_ID = 5L;
    /** 圈子成员角色 ID（sys_role.id=6，CIRCLE_MEMBER） */
    public static final long CIRCLE_MEMBER_ROLE_ID = 6L;

    private CircleRoleConstants() {
    }
}
