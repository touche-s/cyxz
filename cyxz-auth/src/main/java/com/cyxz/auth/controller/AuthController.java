package com.cyxz.auth.controller;

import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;
import com.cyxz.auth.service.AuthService;
import com.cyxz.common.base.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return Result.success("登出成功");
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail(401, "无效的Token");
        }
        String token = authHeader.substring(7);
        AuthResponse response = authService.refreshToken(token);
        return Result.success("刷新成功", response);
    }
}
