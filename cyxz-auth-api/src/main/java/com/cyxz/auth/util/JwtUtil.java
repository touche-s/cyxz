package com.cyxz.auth.util;

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

@Slf4j
@Component
public class JwtUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private String secret;
    private long expirationSeconds;

    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void init(String secret, long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public void init(String secret) {
        this.secret = secret;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

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

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public boolean validateToken(String token) {
        try {
            return !isBlacklisted(token) && !isExpired(token);
        } catch (Exception e) {
            log.warn("Token 验签失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean isExpired(String token) {
        Date expiration = getExpiration(token);
        return expiration.before(new Date());
    }

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
