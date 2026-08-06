package com.cyxz.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 信任头认证过滤器
 * <p>读取 Gateway 注入的信任头，组装 SpringSecurity 的 {@link Authentication} 写入 SecurityContext。
 * <ul>
 *   <li>{@code X-User-Id}：用户 ID（principal）</li>
 *   <li>{@code X-User-Roles}：逗号分隔的全局角色 code，封装为 {@code ROLE_<code>} 供 {@code hasRole()} 使用</li>
 *   <li>{@code X-User-Permissions}：逗号分隔的全局权限码，无前缀供 {@code hasAuthority()} 使用</li>
 * </ul>
 * <p>兼容迁移期：当 {@code X-User-Roles} 缺失时回退读取单值 {@code X-User-Role} 头。
 * <p>圈子内权限不进 SecurityContext，由 {@code @circlePerm} 实时查库校验。
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String LEGACY_ROLE_HEADER = "X-User-Role";
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userIdHeader)) {
            try {
                Long userId = Long.valueOf(userIdHeader);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, buildAuthorities(request)));
            } catch (NumberFormatException ignored) {
                // 非法头忽略，保持未认证状态
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头组装权限集合：角色加 ROLE_ 前缀，权限码原样。
     */
    private List<GrantedAuthority> buildAuthorities(HttpServletRequest request) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 优先读多值 X-User-Roles，缺失则回退单值 X-User-Role（迁移期兼容）
        String roles = request.getHeader(ROLES_HEADER);
        if (!StringUtils.hasText(roles)) {
            roles = request.getHeader(LEGACY_ROLE_HEADER);
        }
        if (StringUtils.hasText(roles)) {
            Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .forEach(authorities::add);
        }
        String permissions = request.getHeader(PERMISSIONS_HEADER);
        if (StringUtils.hasText(permissions)) {
            Arrays.stream(permissions.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
        return authorities;
    }
}
