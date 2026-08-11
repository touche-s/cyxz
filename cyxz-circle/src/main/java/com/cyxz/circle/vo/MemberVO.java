package com.cyxz.circle.vo;

import lombok.Data;

/**
 * 圈子成员 VO，用于圈子管理后台的成员列表
 */
@Data
public class MemberVO {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色 code：CIRCLE_OWNER / CIRCLE_ADMIN / CIRCLE_MEMBER */
    private String roleCode;

    /** 角色中文名称：圈主 / 圈子管理员 / 圈子成员 */
    private String roleLabel;

    /** 加入时间 */
    private String joinTime;
}
