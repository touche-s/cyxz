package com.cyxz.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;
import com.cyxz.auth.entity.SysUserPO;
import com.cyxz.auth.mapper.SysUserMapper;
import com.cyxz.auth.service.AuthService;
import com.cyxz.auth.util.JwtUtil;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";

    @Override
    public AuthResponse login(LoginRequest request) {
        validateCaptcha(request.getCaptcha(), request.getCaptchaUuid());

        SysUserPO user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        long expiresIn = jwtUtil.getExpirationSeconds();

        String cacheKey = "user:login:" + user.getId();
        stringRedisTemplate.opsForValue().set(cacheKey, "1", 5, TimeUnit.MINUTES);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new AuthResponse(token, "Bearer", expiresIn, user.getId(), user.getUsername());
    }

    @Override
    public void register(RegisterRequest request) {
        validateCaptcha(request.getCaptcha(), request.getCaptchaUuid());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        SysUserPO user = new SysUserPO();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        sysUserMapper.insert(user);

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        Long userId = jwtUtil.getUserId(token);
        stringRedisTemplate.delete("user:login:" + userId);
        log.info("用户登出成功: userId={}", userId);
    }

    @Override
    public AuthResponse refreshToken(String oldToken) {
        if (!jwtUtil.validateToken(oldToken) || jwtUtil.isExpired(oldToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(oldToken);

        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        jwtUtil.blacklistToken(oldToken);

        String newToken = jwtUtil.generateToken(userId);
        long expiresIn = jwtUtil.getExpirationSeconds();
        return new AuthResponse(newToken, "Bearer", expiresIn, userId, user.getUsername());
    }

    private void validateCaptcha(String captcha, String captchaUuid) {
        if (!StringUtils.hasText(captcha) || !StringUtils.hasText(captchaUuid)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "验证码不能为空");
        }
        String key = CAPTCHA_PREFIX + captchaUuid;
        String storedCaptcha = stringRedisTemplate.opsForValue().get(key);
        if (storedCaptcha == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
        }
        if (!storedCaptcha.equalsIgnoreCase(captcha)) {
            stringRedisTemplate.delete(key);
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
        stringRedisTemplate.delete(key);
    }
}
