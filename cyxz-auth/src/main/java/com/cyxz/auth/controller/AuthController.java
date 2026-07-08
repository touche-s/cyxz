package com.cyxz.auth.controller;

import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;
import com.cyxz.auth.service.AuthService;
import com.cyxz.auth.util.TokenUtil;
import com.cyxz.common.base.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口控制器
 * <p>处理用户登录、注册、登出和 Token 刷新等认证相关请求。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * <p>校验账号密码和验证码，通过则返回 JWT Token。
     *
     * @param request 登录请求（username + password + captcha）
     * @return 包含 accessToken 和用户信息的认证响应
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     * <p>校验验证码和两次密码一致性，查重后创建账号。
     *
     * @param request 注册请求
     * @return 操作结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    /**
     * 用户登出
     * <p>将 Token 加入 Redis 黑名单，之后该 Token 不可再用于认证。
     *
     * @param authHeader Authorization 请求头
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = TokenUtil.extractBearerToken(authHeader);
        if (token == null) {
            return Result.fail(401, "无效的Token");
        }
        authService.logout(token);
        return Result.success("登出成功");
    }

    /**
     * 刷新 Token
     * <p>使用未过期的旧 Token 换取新 Token，旧 Token 同时失效。
     *
     * @param authHeader Authorization 请求头
     * @return 新的认证响应
     */
    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = TokenUtil.extractBearerToken(authHeader);
        if (token == null) {
            return Result.fail(401, "无效的Token");
        }
        AuthResponse response = authService.refreshToken(token);
        return Result.success("刷新成功", response);
    }
}
