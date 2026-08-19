package com.cyxz.common.security;

import com.cyxz.common.base.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * 权限查询端口
 * <p>抽象 RBAC 数据的读取入口，屏蔽「本地 Mapper 直查」与「Feign 调用 auth 服务」两种实现：
 * <ul>
 *   <li>auth 服务：提供本地实现（{@code AuthPermissionPortImpl}），基于自身 Mapper 直查本库</li>
 *   <li>其他服务：{@code AuthFeignClient} 继承本接口，Feign 调用 {@code /auth/internal/**}</li>
 * </ul>
 * 方法上的 Spring MVC 注解同时作为 Feign 契约（SpringMvcContract 支持接口继承），
 * 返回 {@link Result} 与项目现有 internal 接口契约保持一致（错误码通过 HTTP 200 + Result 传递）。
 */
public interface AuthPermissionPort {

    /**
     * 查询用户的全局角色 code 列表（circle_id=0）
     */
    @GetMapping("/auth/internal/permissions/global-roles")
    Result<List<String>> selectGlobalRoleCodes(@RequestParam("userId") Long userId);

    /**
     * 查询用户的全局权限码列表（circle_id=0 的全局角色关联的权限）
     */
    @GetMapping("/auth/internal/permissions/global-permissions")
    Result<List<String>> selectGlobalPermissionCodes(@RequestParam("userId") Long userId);

    /**
     * 查询用户在指定圈子内的角色 ID 列表
     */
    @GetMapping("/auth/internal/permissions/circle-role-ids")
    Result<List<Long>> selectCircleRoleIds(@RequestParam("userId") Long userId,
                                           @RequestParam("circleId") Long circleId);

    /**
     * 根据角色 ID 集合查询其拥有的权限码集合
     */
    @PostMapping("/auth/internal/permissions/role-permissions")
    Result<Set<String>> selectPermissionCodes(@RequestBody List<Long> roleIds);
}
