package com.cyxz.auth.util;

import com.cyxz.common.CacheKeyConstants;
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
 * <p>负责 Token 的签发、解析、验签以及 Redis 黑名单管理
 */
@Slf4j
@Component
public class JwtUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private String secret;
    private long expirationSeconds;

    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置 JWT 密钥（由配置类注入）
     */
    public void init(String secret, long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * 获取密钥对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

    // ==================== Token 签发 ====================

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    // ==================== Token 解析 ====================

    /**
     * 解析 Token 获取 Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
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
     * 获取 Token 唯一标识
     *
     * @param token JWT Token
     * @return jti
     */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    // ==================== Token 验签 ====================

    /**
     * 验证 Token 是否有效（签名正确 + 未过期 + 未加入黑名单）
     *
     * @param token JWT Token
     * @return 是否有效
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
     * 检查 Token 是否已过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isExpired(String token) {
        Date expiration = getExpiration(token);
        return expiration.before(new Date());
    }

    // ==================== 黑名单管理 ====================

    /**
     * 将 Token 加入黑名单（退出登录）
     *
     * @param token JWT Token
     */
    public void blacklistToken(String token) {
        try {
            String jti = getJti(token);
            Date expiration = getExpiration(token);
            long ttl = Math.max((expiration.getTime() - System.currentTimeMillis()) / 1000, 1);
            String key = CacheKeyConstants.TOKEN_BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
            log.info("Token 已加入黑名单, jti={}, ttl={}s", jti, ttl);
        } catch (Exception e) {
            log.error("Token 加入黑名单失败", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return 是否在黑名单
     */
    public boolean isBlacklisted(String token) {
        try {
            String jti = getJti(token);
            String key = CacheKeyConstants.TOKEN_BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("检查黑名单失败: {}", e.getMessage());
            return false;
        }
    }
}
