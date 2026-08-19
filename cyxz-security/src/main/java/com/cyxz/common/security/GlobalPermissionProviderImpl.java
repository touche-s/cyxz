package com.cyxz.common.security;

import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全局权限提供者实现（Cache-Aside 旁观策略）
 * <p>读链路：Redis 命中 → 直接返回；未命中 → 经 {@link AuthPermissionPort} 查 auth → 回写 Redis（TTL 对齐当前 Token 剩余时间）。
 * <p>降级：Redis 异常 → 走 auth 查询返回（不写缓存）；auth 查询失败（Feign 降级返回空集）→ 保持未认证状态（拒绝放行）。
 * <p>非自动注册组件：在需要完整权限的服务的 SecurityConfig 中通过 {@code @Bean} 手动声明。
 */
@Slf4j
@RequiredArgsConstructor
public class GlobalPermissionProviderImpl implements GlobalPermissionProvider {

    private final AuthPermissionPort authPermissionPort;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String HASH_FIELD_ROLES = "roles";
    private static final String HASH_FIELD_PERMS = "perms";

    @Override
    public Set<String> getRoles(Long userId) {
        String key = CacheKeyConstants.getAuthGlobalKey(userId);
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, HASH_FIELD_ROLES);
            if (cached != null) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询全局角色失败，降级查 auth: userId={}", userId, e);
        }
        // auth 查询
        Result<List<String>> result = authPermissionPort.selectGlobalRoleCodes(userId);
        Set<String> roles;
        if (result != null && result.isSuccess() && result.getData() != null) {
            roles = new HashSet<>(result.getData());
        } else {
            log.warn("auth 查询全局角色失败或降级，保持空权限: userId={}", userId);
            roles = Set.of();
        }
        Set<String> effectiveRoles = roles.isEmpty() ? Set.of("USER") : roles;
        // 回写 Redis，TTL 对齐当前 Token 剩余时间
        try {
            stringRedisTemplate.opsForHash().put(key, HASH_FIELD_ROLES, String.join(",", effectiveRoles));
            applyTtl(key);
        } catch (Exception e) {
            log.warn("Redis 回写全局角色失败: userId={}", userId, e);
        }
        return effectiveRoles;
    }

    @Override
    public Set<String> getPermissions(Long userId) {
        String key = CacheKeyConstants.getAuthGlobalKey(userId);
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, HASH_FIELD_PERMS);
            if (cached != null) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询全局权限失败，降级查 auth: userId={}", userId, e);
        }
        // auth 查询
        Result<List<String>> result = authPermissionPort.selectGlobalPermissionCodes(userId);
        Set<String> permissions;
        if (result != null && result.isSuccess() && result.getData() != null) {
            permissions = new HashSet<>(result.getData());
        } else {
            log.warn("auth 查询全局权限失败或降级，保持空权限: userId={}", userId);
            permissions = Set.of();
        }
        // 回写 Redis，TTL 对齐当前 Token 剩余时间
        try {
            stringRedisTemplate.opsForHash().put(key, HASH_FIELD_PERMS, String.join(",", permissions));
            applyTtl(key);
        } catch (Exception e) {
            log.warn("Redis 回写全局权限失败: userId={}", userId, e);
        }
        return permissions;
    }

    /**
     * 用当前 Token 剩余秒数设置 Redis Key 的 TTL，使权限缓存与 Token 同步过期。
     * <p>roles 和 perms 共享同一个 Hash Key，先写入的 field 会设置 TTL，后写入的 field 刷新 TTL ——
     * 两者都来自同一个 Token，剩余时间一致，不会出现冲突。
     */
    private void applyTtl(String key) {
        long ttl = TokenTtlContext.get();
        if (ttl > 0) {
            stringRedisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        }
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
