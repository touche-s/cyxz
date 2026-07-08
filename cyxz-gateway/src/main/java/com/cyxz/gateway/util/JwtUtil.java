package com.cyxz.gateway.util;

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

/**
 * JWT 工具类（网关专用）
 * <p>负责 Token 的解析、验签以及 Redis 黑名单检查。
 * <p>注意：Token 签发由 cyxz-auth 服务负责，网关只做校验。
 */
@Slf4j
@Component
public class JwtUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private String secret;

    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置 JWT 密钥（由配置类注入）
     *
     * @param secret JWT 签名密钥
     */
    public void init(String secret) {
        this.secret = secret;
    }

    /**
     * 获取密钥对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
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

     /* 检查 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return 是否在黑名单
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
