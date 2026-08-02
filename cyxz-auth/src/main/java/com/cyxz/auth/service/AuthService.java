package com.cyxz.auth.service;

import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.ChangePasswordRequest;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;

/**
 * 认证服务接口
 * <p>提供登录、注册、登出、改密和 Token 刷新的核心方法。
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 包含 Token 的认证响应
     */
    AuthResponse login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 用户登出
     *
     * @param token 待失效的 Token
     */
    void logout(String token);

    /**
     * 修改密码
     * <p>校验旧密码正确后，用 BCrypt 加密新密码并更新。
     *
     * @param userId  当前登录用户 ID
     * @param request 改密请求（旧密码 + 新密码 + 确认密码）
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    Long extractUserId(String token);

    /**
     * 刷新 Token
     *
     * @param oldToken 旧 Token（需有效）
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String oldToken);
}
