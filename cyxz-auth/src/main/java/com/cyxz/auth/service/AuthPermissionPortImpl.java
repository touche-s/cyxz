package com.cyxz.auth.service;

import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.common.base.Result;
import com.cyxz.common.security.AuthPermissionPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 权限查询端口本地实现（auth 服务专用）
 * <p>auth 服务自己是 RBAC 表 owner，直接查本库即可，无需经 Feign 自环调用。
 * 其他服务由 {@code AuthFeignClient}（Feign 继承 {@link AuthPermissionPort}）提供等价契约。
 * <p>非自动注册组件：在 auth 的 {@code SecurityConfig} 中通过 {@code @Bean} 显式声明，
 * 避免与 Feign 契约接口产生类型歧义。
 */
@RequiredArgsConstructor
public class AuthPermissionPortImpl implements AuthPermissionPort {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public Result<List<String>> selectGlobalRoleCodes(Long userId) {
        return Result.success(sysUserRoleMapper.selectGlobalRoleCodes(userId));
    }

    @Override
    public Result<List<String>> selectGlobalPermissionCodes(Long userId) {
        return Result.success(sysUserRoleMapper.selectGlobalPermissionCodes(userId));
    }

    @Override
    public Result<List<Long>> selectCircleRoleIds(Long userId, Long circleId) {
        return Result.success(sysUserRoleMapper.selectCircleRoleIds(userId, circleId));
    }

    @Override
    public Result<Set<String>> selectPermissionCodes(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Result.success(Set.of());
        }
        return Result.success(sysUserRoleMapper.selectPermissionCodes(roleIds));
    }
}
