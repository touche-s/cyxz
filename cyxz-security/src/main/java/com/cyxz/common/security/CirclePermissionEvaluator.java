package com.cyxz.common.security;

import com.cyxz.circle.feign.CircleExistsPort;
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
 * 圈子数据权限校验器
 * <p>SpEL Bean，供 {@code @PreAuthorize("@circlePerm.hasAuthority('circle:post:review', #circleId)")} 调用。
 * <p>非自动注册组件：仅在需要圈子权限校验的业务服务（circle/post/upload）的 SecurityConfig 中
 * 通过 {@code @Bean(name="circlePerm")} 手动声明，避免被不需要该能力的服务扫描加载。
 * <p>校验链路：
 * <ol>
 *   <li>未登录 → false</li>
 *   <li>全局管理员（站主/平台管理员）→ true，短路返回</li>
 *   <li>circleId 为空 → false</li>
 *   <li>圈子存在性经 {@link CircleExistsPort} 校验（circle 域归属，防传不存在的 ID 绕过；
 *       circle 服务用本地实现，其他服务走 {@code CircleFeignClient} Feign 调用）</li>
 *   <li>Cache-Aside：Redis 查圈子权限 → 命中则判断；未命中 → 经 {@link AuthPermissionPort} 查 auth → 回写 Redis</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class CirclePermissionEvaluator {

    private final AuthPermissionPort authPermissionPort;
    private final CircleExistsPort circleExistsPort;
    private final StringRedisTemplate stringRedisTemplate;

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
        // 校验圈子真实存在，防传不存在的 ID 绕过（走 circle 域）
        Result<Boolean> existsResult = circleExistsPort.exists(circleId);
        if (existsResult == null || !existsResult.isSuccess() || !Boolean.TRUE.equals(existsResult.getData())) {
            return false;
        }

        Set<String> permissions = loadCirclePermissions(userId, circleId);
        return permissions.contains(permissionCode);
    }

    /**
     * Cache-Aside 加载用户在圈子内的权限码集合，TTL 对齐当前 Token 剩余时间
     */
    private Set<String> loadCirclePermissions(Long userId, Long circleId) {
        String key = CacheKeyConstants.getAuthCircleKey(userId, circleId);
        // 尝试 Redis
        try {
            String cached = (String) stringRedisTemplate.opsForHash().get(key, "perms");
            if (cached != null) {
                return parseCsv(cached);
            }
        } catch (Exception e) {
            log.warn("Redis 查询圈子权限失败，降级查 auth: userId={}, circleId={}", userId, circleId, e);
        }
        // auth 查询
        Result<List<Long>> roleResult = authPermissionPort.selectCircleRoleIds(userId, circleId);
        Set<String> permissions;
        if (roleResult == null || !roleResult.isSuccess()) {
            log.warn("auth 查询圈子角色失败或降级，视为无圈子权限: userId={}, circleId={}", userId, circleId);
            permissions = Set.of();
        } else {
            List<Long> roleIds = roleResult.getData();
            if (roleIds == null || roleIds.isEmpty()) {
                permissions = Set.of();
            } else {
                Result<Set<String>> permResult = authPermissionPort.selectPermissionCodes(roleIds);
                permissions = permResult != null && permResult.isSuccess() && permResult.getData() != null
                        ? permResult.getData()
                        : Set.of();
            }
        }
        // 回写 Redis
        try {
            stringRedisTemplate.opsForHash().put(key, "perms", String.join(",", permissions));
            long ttl = TokenTtlContext.get();
            if (ttl > 0) {
                stringRedisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Redis 回写圈子权限失败: userId={}, circleId={}", userId, circleId, e);
        }
        return permissions;
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
