package com.cyxz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /** 全局角色 code：SITE_OWNER / PLATFORM_ADMIN / USER */
    private String role;

    /** 全局权限码列表（随 JWT 下发，供前端 UI 显隐与网关透传） */
    private List<String> permissions;
}
