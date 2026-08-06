package com.cyxz.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全上下文工具类
 * <p>从 {@link SecurityContextHolder} 读取当前用户身份与权限，供业务代码与 SpEL 组件使用。
 */
public final class SecurityUtils {

    /** 全局管理员角色 code 集合：站主、平台管理员 */
    public static final Set<String> GLOBAL_ADMIN_ROLES = Set.of("SITE_OWNER", "PLATFORM_ADMIN");

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未认证返回 null
     */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 判断当前用户是否为全局管理员（站主/平台管理员）
     *
     * @return true=全局管理员
     */
    public static boolean isGlobalAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.startsWith("ROLE_")
                        && GLOBAL_ADMIN_ROLES.contains(a.substring(5)));
    }

    /**
     * 判断当前用户是否拥有指定全局权限码
     *
     * @param permissionCode 权限码
     * @return true=拥有
     */
    public static boolean hasAuthority(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permissionCode::equals);
    }

    /**
     * 获取当前用户的全局角色 code 集合（去除 ROLE_ 前缀）
     *
     * @return 角色 code 集合，未认证返回空集
     */
    public static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());
    }
}
