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
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.user.feign.UserFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现
 * <p>处理登录、注册、登出和 Token 刷新的业务逻辑。
 * 不使用 Spring Security 过滤器链，直接查表校验密码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserFeignClient userFeignClient;

    /**
     * 用户登录
     * <p>1. 校验验证码 2. 查用户 3. BCrypt 比对密码 4. 签发 JWT
     *
     * @param request 登录请求
     * @return 认证响应（token + userId + username）
     */
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

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new AuthResponse(token, "Bearer", expiresIn, user.getId(), user.getUsername());
    }

    /**
     * 用户注册
     * <p>1. 校验验证码 2. 校验两次密码一致 3. 查重 4. BCrypt 加密入库
     *
     * @param request 注册请求
     */
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
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        // 初始化默认资料，失败不阻塞注册流程（降级由 FallbackFactory 处理）
        Result<Void> initResult = userFeignClient.initDefaultProfile(user.getId(), user.getUsername());
        if (initResult == null || !initResult.isSuccess()) {
            log.error("初始化用户资料失败，需人工补偿: userId={}, username={}", user.getId(), user.getUsername());
        }
    }

    /**
     * 用户登出
     * <p>Token 加入 Redis 黑名单。
     *
     * @param token 待失效的 Token
     */
    @Override
    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        log.info("用户登出成功: userId={}", jwtUtil.getUserId(token));
    }

    /**
     * 刷新 Token
     * <p>校验旧 Token 有效后签发新 Token，旧 Token 同时失效。
     *
     * @param oldToken 旧 Token（需未过期且不在黑名单）
     * @return 新的认证响应
     */
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

    /**
     * 校验图形验证码
     * <p>从 Redis 中根据 captchaUuid 取出存储的验证码，与用户输入进行比对（忽略大小写）。
     * 校验通过后删除验证码（一次性有效），失败则抛出对应异常。
     *
     * @param captcha     用户输入的验证码
     * @param captchaUuid 验证码唯一标识（前端返回的 UUID）
     */
    private void validateCaptcha(String captcha, String captchaUuid) {
        if (!StringUtils.hasText(captcha) || !StringUtils.hasText(captchaUuid)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "验证码不能为空");
        }
        String key = CacheKeyConstants.getCaptchaKey(captchaUuid);
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
