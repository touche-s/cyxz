package com.cyxz.auth.utils;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 * <p>提供 Token 生成、解析、校验、黑名单管理等功能。
 * <p>auth 服务通过 {@link #init(String, long)} 注入 secret 和过期时间；
 * gateway 通过 {@link #init(String)} 仅注入 secret 用于验签。
 * <p>黑名单使用 Redis 存储，Key 为 token 的 jti，TTL 对齐 token 剩余有效期。
 */
@Slf4j
@Component
public class JwtUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private String secret;
    private long expirationSeconds;

    /**
     * 通过构造器注入 RedisTemplate
     *
     * @param redisTemplate Redis 操作模板
     */
    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final int MIN_KEY_BYTES = 32;

    /**
     * 初始化 secret 和 token 过期时间（供 auth 服务调用）
     *
     * @param secret            JWT 签名密钥
     * @param expirationSeconds Token 过期时间（秒）
     */
    public void init(String secret, long expirationSeconds) {
        checkKeyLength(secret);
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * 初始化 secret（供 gateway 调用，仅需验签无需过期时间）
     *
     * @param secret JWT 签名密钥
     */
    public void init(String secret) {
        checkKeyLength(secret);
        this.secret = secret;
    }

    private void checkKeyLength(String secret) {
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_KEY_BYTES) {
            throw new IllegalArgumentException(
                "JWT 密钥长度不足: " + bytes + " 字节, HMAC-SHA256 要求至少 " + MIN_KEY_BYTES + " 字节");
        }
    }

    /**
     * 获取 HMAC-SHA256 签名密钥
     *
     * @return SecretKey 实例
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

    /**
     * 生成 JWT Token
     * <p>Payload 包含 userId(sub)、jti、iat、exp，使用 HMAC-SHA256 签名。
     *
     * @param userId 用户 ID
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId) {
        return generateToken(userId, "user");
    }

    /**
     * 生成 JWT Token（含角色）
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, String role) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析并校验 Token 签名，返回 Claims
     *
     * @param token JWT Token
     * @return Token 中的 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中提取用户角色
     *
     * @param token JWT Token
     * @return 角色字符串，默认 "user"
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "user";
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取 Token 的 JTI（JWT ID）
     *
     * @param token JWT Token
     * @return JTI 字符串
     */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * 获取配置的 Token 过期时间
     *
     * @return 过期时间（秒）
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * 校验 Token 是否有效
     * <p>同时检查黑名单和过期状态，验签失败也视为无效。
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            return !isBlacklisted(token) && !isExpired(token);
        } catch (Exception e) {
            log.warn("Token 验签失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断 Token 是否已过期
     *
     * @param token JWT Token
     * @return true-已过期
     */
    public boolean isExpired(String token) {
        Date expiration = getExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * 将 Token 加入 Redis 黑名单
     * <p>使用 jti 作为 Key，TTL 对齐 token 剩余有效期（至少 1 秒）。
     * 操作失败时抛出 {@link ErrorCode#SYSTEM_ERROR} 异常。
     *
     * @param token JWT Token
     */
    public void blacklistToken(String token) {
        try {
            String jti = getJti(token);
            Date expiration = getExpiration(token);
            long ttl = Math.max((expiration.getTime() - System.currentTimeMillis()) / 1000, 1);
            String key = CacheKeyConstants.getTokenBlacklistKey(jti);
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
            log.info("Token 已加入黑名单, jti={}, ttl={}s", jti, ttl);
        } catch (Exception e) {
            log.error("Token 加入黑名单失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Token失效失败，请稍后重试", e);
        }
    }

    /**
     * 判断 Token 是否在黑名单中
     * <p>Redis 操作异常时保守处理，返回 true 阻止使用。
     *
     * @param token JWT Token
     * @return true-已加入黑名单
     */
    public boolean isBlacklisted(String token) {
        try {
            String jti = getJti(token);
            String key = CacheKeyConstants.getTokenBlacklistKey(jti);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("检查黑名单失败: {}", e.getMessage());
            return true;
        }
    }
}
