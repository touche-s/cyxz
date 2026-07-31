package com.cyxz.auth.vo;

import lombok.Data;

/**
 * 管理员用户列表 VO
 */
@Data
public class UserAdminVO {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
}
