package com.cyxz.auth.service;

import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    void register(RegisterRequest request);

    void logout(String token);

    AuthResponse refreshToken(String oldToken);
}
