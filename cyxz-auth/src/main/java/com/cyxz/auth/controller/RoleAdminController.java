package com.cyxz.auth.controller;

import com.cyxz.auth.service.RoleAdminService;
import com.cyxz.auth.vo.PermissionVO;
import com.cyxz.auth.vo.RoleVO;
import com.cyxz.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RBAC 管理（管理员端）
 * <p>角色列表、权限点列表、角色-权限分配、用户全局角色分配。
 */
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    /**
     * 查询全部角色列表
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:manage:list')")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(roleAdminService.listRoles());
    }

    /**
     * 查询全部权限点列表
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:manage:list')")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(roleAdminService.listPermissions());
    }

    /**
     * 查询角色已分配的权限 ID 列表
     */
    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('role:manage:list')")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(roleAdminService.getRolePermissionIds(roleId));
    }

    /**
     * 更新角色的权限分配（全量覆盖）
     *
     * @param roleId 角色 ID
     * @param body   请求体，包含 permissionIds（权限 ID 列表）
     */
    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('role:manage:assign')")
    public Result<Void> updateRolePermissions(@PathVariable Long roleId,
                                              @RequestBody Map<String, List<Long>> body) {
        roleAdminService.updateRolePermissions(roleId, body.get("permissionIds"));
        return Result.success("权限分配更新成功");
    }

    /**
     * 分配用户的全局角色
     *
     * @param userId 目标用户 ID
     * @param body   请求体，包含 roleId（角色 ID）
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('role:manage:assign')")
    public Result<Void> assignUserRole(@PathVariable Long userId,
                                       @RequestBody Map<String, Long> body) {
        roleAdminService.assignUserGlobalRole(userId, body.get("roleId"));
        return Result.success("角色分配成功");
    }
}
