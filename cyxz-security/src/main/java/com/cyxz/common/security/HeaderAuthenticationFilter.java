package com.cyxz.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 信任头认证过滤器
 * <p>读取 Gateway 注入的 {@code X-User-Id} 和 {@code X-Token-Remaining}，通过 {@link GlobalPermissionProvider}
 * 从 Redis/DB 加载全局角色和权限码，组装 Spring Security 的 {@link Authentication} 写入 SecurityContext。
 * <p>Token 剩余秒数存入 {@link TokenTtlContext}，供权限缓存层回写 Redis 时对齐 TTL。
 * <p>圈子内权限不进 SecurityContext，由 {@code @circlePerm} 实时查库校验。
 */
@Slf4j
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String TOKEN_REMAINING_HEADER = "X-Token-Remaining";

    private final GlobalPermissionProvider globalPermissionProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 内部接口（/internal/）不加载权限：服务间 Feign 直连时若在此加载权限，
        // 会经 AuthPermissionPort 再次调用 auth 服务造成递归；且 internal 接口本身不依赖 SecurityContext。
        if (request.getRequestURI() != null && request.getRequestURI().contains("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userIdHeader)) {
            try {
                Long userId = Long.valueOf(userIdHeader);
                long remainingSeconds = parseRemainingSeconds(request.getHeader(TOKEN_REMAINING_HEADER));
                TokenTtlContext.set(remainingSeconds);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, buildAuthorities(userId)));
            } catch (NumberFormatException ignored) {
                // 非法头忽略，保持未认证状态
            } catch (Exception e) {
                log.warn("加载用户权限失败，保持未认证状态: userIdHeader={}", userIdHeader, e);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TokenTtlContext.clear();
        }
    }

    private long parseRemainingSeconds(String header) {
        if (!StringUtils.hasText(header)) {
            return 0;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从 GlobalPermissionProvider 加载角色和权限，组装 GrantedAuthority 列表。
     * 角色加 ROLE_ 前缀供 hasRole() 使用，权限码原样供 hasAuthority() 使用。
     */
    private List<GrantedAuthority> buildAuthorities(Long userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Set<String> roles = globalPermissionProvider.getRoles(userId);
        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

        Set<String> permissions = globalPermissionProvider.getPermissions(userId);
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }
}
