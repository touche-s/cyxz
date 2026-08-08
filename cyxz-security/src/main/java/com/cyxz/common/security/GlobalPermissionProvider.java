package com.cyxz.common.security;

import java.util.Set;

/**
 * 全局权限提供者
 * <p>负责从 Redis/DB 加载用户的全局角色和权限码，供 {@link HeaderAuthenticationFilter} 构造 SecurityContext。
 * <p>采用 Cache-Aside 旁观策略：读未命中 → 查 DB → 回写 Redis；权限变更时主动删 key。
 */
public interface GlobalPermissionProvider {

    /**
     * 获取用户全局角色 code 集合
     *
     * @param userId 用户 ID
     * @return 角色 code 集合，如 [SITE_OWNER, PLATFORM_ADMIN]
     */
    Set<String> getRoles(Long userId);

    /**
     * 获取用户全局权限码集合
     *
     * @param userId 用户 ID
     * @return 权限码集合，如 [post:review:list, post:admin:delete]
     */
    Set<String> getPermissions(Long userId);
}
