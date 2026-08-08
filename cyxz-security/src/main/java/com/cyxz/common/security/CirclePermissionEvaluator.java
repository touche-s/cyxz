package com.cyxz.common.security;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.security.mapper.PermissionQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 圈子数据权限校验器
 * <p>SpEL Bean，供 {@code @PreAuthorize("@circlePerm.hasAuthority('circle:post:review', #circleId)")} 调用。
 * <p>非自动注册组件：仅在需要圈子权限校验的业务服务（circle/post/upload）的 SecurityConfig 中
 * 通过 {@code @Bean(name="circlePerm")} 手动声明，避免被不需要该能力的服务扫描加载。
 * <p>校验链路：
 * <ol>
 *   <li>未登录 → false</li>
 *   <li>全局管理员（站主/平台管理员）→ true，短路返回</li>
 *   <li>circleId 为空或圈子不存在 → false（防传不存在的 ID 绕过）</li>
 *   <li>Cache-Aside：Redis 查圈子权限 → 命中则判断；未命中 → 查 DB → 回写 Redis</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class CirclePermissionEvaluator {

    private final PermissionQueryMapper permissionQueryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final long ttlSeconds;

    /**
     * 校验当前用户在指定圈子内是否拥有某权限码
     *
     * @param permissionCode 权限码，如 {@code circle:post:review}
     * @param circleId       圈子 ID
     * @return true=有权限
     */
    public boolean hasAuthority(String permissionCode, Long circleId) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            return false;
        }
        // 全局管理员对任意圈子都有权限
        if (SecurityUtils.isGlobalAdmin()) {
            return true;
        }
        if (circleId == null) {
            return false;
        }
        // 校验圈子真实存在，防传不存在的 ID 绕过
        if (permissionQueryMapper.countCircleById(circleId) == 0) {
            return false;
        }

        Set<String> permissions = loadCirclePermissions(userId, circleId);
        return permissions.contains(permissionCode);
    }

    /**
     * Cache-Aside 加载用户在圈子内的权限码集合
     */
    private Set<String> loadCirclePermissions(Long userId, Long circleId) {
        String key = CacheKeyConstants.getAuthCircleKey(userId, circleId);
        // 尝试 Redis
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, "perms");
            if (cached != null && !cached.isEmpty()) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询圈子权限失败，降级查 DB: userId={}, circleId={}", userId, circleId, e);
        }
        // DB 查询
        List<Long> roleIds = permissionQueryMapper.selectCircleRoleIds(userId, circleId);
        Set<String> permissions;
        if (roleIds.isEmpty()) {
            permissions = Set.of();
        } else {
            permissions = permissionQueryMapper.selectPermissionCodes(roleIds);
        }
        // 回写 Redis
        try {
            stringRedisTemplate.opsForHash().put(key, "perms", String.join(",", permissions));
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 回写圈子权限失败: userId={}, circleId={}", userId, circleId, e);
        }
        return permissions;
    }

    private Set<String> parseCsv(String csv) {
        Set<String> result = new java.util.HashSet<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
