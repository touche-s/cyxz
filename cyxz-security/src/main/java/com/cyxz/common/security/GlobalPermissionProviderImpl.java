package com.cyxz.common.security;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.security.mapper.PermissionQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全局权限提供者实现（Cache-Aside 旁观策略）
 * <p>读链路：Redis 命中 → 直接返回；未命中 → 查 DB → 回写 Redis（TTL 对齐 JWT 过期时间）。
 * <p>降级：Redis 异常 → 查 DB 返回（不写缓存）；Redis + DB 都异常 → 返回空集（拒绝放行）。
 * <p>非自动注册组件：在需要完整权限的服务的 SecurityConfig 中通过 {@code @Bean} 手动声明。
 */
@Slf4j
@RequiredArgsConstructor
public class GlobalPermissionProviderImpl implements GlobalPermissionProvider {

    private final PermissionQueryMapper permissionQueryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final long ttlSeconds;

    private static final String HASH_FIELD_ROLES = "roles";
    private static final String HASH_FIELD_PERMS = "perms";

    @Override
    public Set<String> getRoles(Long userId) {
        String key = CacheKeyConstants.getAuthGlobalKey(userId);
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, HASH_FIELD_ROLES);
            if (cached != null && !cached.isEmpty()) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询全局角色失败，降级查 DB: userId={}", userId, e);
        }
        // DB 查询
        List<String> roles = permissionQueryMapper.selectGlobalRoleCodes(userId);
        Set<String> result = roles.isEmpty() ? Set.of("USER") : new HashSet<>(roles);
        // 回写 Redis
        try {
            stringRedisTemplate.opsForHash().put(key, HASH_FIELD_ROLES, String.join(",", result));
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 回写全局角色失败: userId={}", userId, e);
        }
        return result;
    }

    @Override
    public Set<String> getPermissions(Long userId) {
        String key = CacheKeyConstants.getAuthGlobalKey(userId);
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, HASH_FIELD_PERMS);
            if (cached != null && !cached.isEmpty()) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询全局权限失败，降级查 DB: userId={}", userId, e);
        }
        // DB 查询
        Set<String> result = permissionQueryMapper.selectGlobalPermissionCodes(userId);
        if (result == null) {
            result = Set.of();
        }
        // 回写 Redis
        try {
            stringRedisTemplate.opsForHash().put(key, HASH_FIELD_PERMS, String.join(",", result));
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 回写全局权限失败: userId={}", userId, e);
        }
        return result;
    }

    private Set<String> parseCsv(String csv) {
        Set<String> result = new HashSet<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
