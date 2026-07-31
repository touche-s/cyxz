package com.cyxz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应
 * <p>登录或 Token 刷新成功后返回的 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT 访问令牌 */
    private String accessToken;

    /** 令牌类型，固定为 "Bearer" */
    private String tokenType;

    /** 令牌有效时长（秒） */
    private Long expiresIn;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色：admin / user */
    private String role;
}
