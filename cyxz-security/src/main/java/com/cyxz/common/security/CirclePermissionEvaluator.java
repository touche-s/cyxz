package com.cyxz.common.security;

import com.cyxz.common.security.mapper.PermissionQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

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
 *   <li>查用户在该圈子的角色 → 查角色对应的权限码 → 判断是否包含目标权限码</li>
 * </ol>
 * <p>基础版直接查 DB（两次索引查询，毫秒级），低并发足够；高并发可演进到 Redis 缓存
 * （key={@code circle:perm:{userId}:{circleId}}，角色变更时主动失效）。
 */
@Slf4j
@RequiredArgsConstructor
public class CirclePermissionEvaluator {

    private final PermissionQueryMapper permissionQueryMapper;

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
        List<Long> roleIds = permissionQueryMapper.selectCircleRoleIds(userId, circleId);
        if (roleIds.isEmpty()) {
            return false;
        }
        Set<String> permissions = permissionQueryMapper.selectPermissionCodes(roleIds);
        return permissions.contains(permissionCode);
    }
}
