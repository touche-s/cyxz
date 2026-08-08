package com.cyxz.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.auth.dto.AuthResponse;
import com.cyxz.auth.dto.ChangePasswordRequest;
import com.cyxz.auth.dto.LoginRequest;
import com.cyxz.auth.dto.RegisterRequest;
import com.cyxz.auth.entity.SysUserPO;
import com.cyxz.auth.entity.SysUserRolePO;
import com.cyxz.auth.mapper.SysUserMapper;
import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.service.AuthService;
import com.cyxz.auth.utils.JwtUtil;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.utils.IpUtil;
import com.cyxz.user.feign.UserFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final SysUserRoleMapper sysUserRoleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserFeignClient userFeignClient;

    /** dummy BCrypt 哈希，用于用户不存在时平衡响应时间（防时间侧信道），懒加载 */
    private volatile String dummyHash;

    /**
     * 用户登录
     * <p>1. IP 失败次数限流 2. 校验验证码 3. 查用户 4. BCrypt 比对密码 5. 签发 JWT
     * <p>用户不存在时跑一次 dummy BCrypt 平衡响应时间，防止通过响应耗时探测账号是否存在。
     * <p>同一 IP 在 5 分钟内登录失败超过 10 次将被锁定。
     *
     * @param request 登录请求
     * @return 认证响应（token + userId + username）
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        // IP 失败次数限流：超过阈值直接拒绝
        checkLoginFailLimit(clientIp);

        validateCaptcha(request.getCaptcha(), request.getCaptchaUuid());

        SysUserPO user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getUsername, request.getUsername())
        );
        if (user == null) {
            // 跑一次 dummy BCrypt 平衡响应时间，防时间侧信道
            passwordEncoder.matches(request.getPassword(), getDummyHash());
            recordLoginFail(clientIp);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFail(clientIp);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }

        // 登录成功，清空失败计数
        clearLoginFail(clientIp);

        String role = getGlobalRoleCode(user.getId());
        List<String> permissions = getGlobalPermissionCodes(user.getId());
        String token = jwtUtil.generateToken(user.getId());
        long expiresIn = jwtUtil.getExpirationSeconds();

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), role);
        return new AuthResponse(token, "Bearer", expiresIn, user.getId(), user.getUsername(), role, permissions);
    }

    /**
     * 检查 IP 登录失败次数是否超限，超限抛 FORBIDDEN
     */
    private void checkLoginFailLimit(String ip) {
        if (ip == null) {
            return;
        }
        String key = CacheKeyConstants.getLoginFailKey(ip);
        String count = stringRedisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count) >= CacheKeyConstants.LOGIN_FAIL_MAX_ATTEMPTS) {
            log.warn("IP 登录失败次数超限，临时锁定: ip={}, count={}", ip, count);
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录失败次数过多，请稍后再试");
        }
    }

    /**
     * 记录一次登录失败，计数 +1 并设置窗口过期
     */
    private void recordLoginFail(String ip) {
        if (ip == null) {
            return;
        }
        String key = CacheKeyConstants.getLoginFailKey(ip);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, CacheKeyConstants.LOGIN_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 登录成功后清空失败计数
     */
    private void clearLoginFail(String ip) {
        if (ip == null) {
            return;
        }
        stringRedisTemplate.delete(CacheKeyConstants.getLoginFailKey(ip));
    }

    /**
     * 从当前请求上下文获取客户端 IP
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest httpRequest = attrs.getRequest();
        return IpUtil.getClientIp(httpRequest);
    }

    /**
     * 查询用户的全局角色 code（circle_id=0）
     * <p>用户应有且仅有一个全局角色（SITE_OWNER / PLATFORM_ADMIN / USER），
     * 无记录时降级为 USER。
     */
    private String getGlobalRoleCode(Long userId) {
        List<String> codes = sysUserRoleMapper.selectGlobalRoleCodes(userId);
        return codes.isEmpty() ? "USER" : codes.get(0);
    }

    /**
     * 查询用户的全局权限码列表（基于 circle_id=0 的全局角色）
     * <p>登录时写入 JWT，普通用户无管理权限返回空列表。
     */
    private List<String> getGlobalPermissionCodes(Long userId) {
        List<String> codes = sysUserRoleMapper.selectGlobalPermissionCodes(userId);
        return codes.isEmpty() ? Collections.emptyList() : codes;
    }

    /**
     * 懒加载生成一个 dummy BCrypt 哈希，用于平衡用户不存在时的响应时间
     */
    private String getDummyHash() {
        if (dummyHash == null) {
            synchronized (this) {
                if (dummyHash == null) {
                    dummyHash = passwordEncoder.encode("dummy-password-for-timing-balance");
                }
            }
        }
        return dummyHash;
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

        // 分配默认全局 USER 角色（role_id=3, circle_id=0）
        SysUserRolePO userRole = new SysUserRolePO();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        userRole.setCircleId(0L);
        sysUserRoleMapper.insert(userRole);

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        // 初始化默认资料，失败不阻塞注册流程（降级由 FallbackFactory 处理）
        Result<Void> initResult = userFeignClient.initDefaultProfile(user.getId(), user.getUsername());
        if (initResult == null || !initResult.isSuccess()) {
            log.error("初始化用户资料失败，需人工补偿: userId={}, username={}", user.getId(), user.getUsername());
        }
    }

    /**
     * 用户登出
     * <p>Token 加入 Redis 黑名单，同时清除全局权限缓存。
     *
     * @param token 待失效的 Token
     */
    @Override
    public void logout(String token) {
        Long userId = jwtUtil.getUserId(token);
        jwtUtil.blacklistToken(token);
        // 清除全局权限缓存
        stringRedisTemplate.delete(CacheKeyConstants.getAuthGlobalKey(userId));
        log.info("用户登出成功: userId={}", userId);
    }

    /**
     * 修改密码
     * <p>1. 校验旧密码正确 2. 校验新密码与确认一致 3. 校验新旧密码不同 4. BCrypt 加密更新
     *
     * @param userId  当前登录用户 ID
     * @param request 改密请求（旧密码 + 新密码 + 确认密码）
     */
    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 校验两次新密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }

        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }

        // 新旧密码不能相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    @Override
    public Long extractUserId(String token) {
        return jwtUtil.getUserId(token);
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
        // 刷新时校验用户状态，禁用用户不能无限刷新 Token
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        jwtUtil.blacklistToken(oldToken);

        String role = getGlobalRoleCode(userId);
        List<String> permissions = getGlobalPermissionCodes(userId);
        String newToken = jwtUtil.generateToken(userId);
        long expiresIn = jwtUtil.getExpirationSeconds();
        return new AuthResponse(newToken, "Bearer", expiresIn, userId, user.getUsername(), role, permissions);
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
