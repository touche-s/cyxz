package com.cyxz.auth.controller;

import com.cyxz.auth.feign.dto.CircleRoleRequest;
import com.cyxz.auth.feign.vo.CircleMemberVO;
import com.cyxz.auth.service.CircleRoleService;
import com.cyxz.auth.vo.CircleMemberRoleVO;
import com.cyxz.common.base.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务内部接口控制器
 * <p>仅供服务间 Feign 直连调用（不经网关），路径统一 {@code /auth/internal/**}：
 * <ul>
 *   <li>权限查询：供各服务 {@code @circlePerm} 与权限缓存加载（AuthPermissionPort 契约）</li>
 *   <li>圈子角色管理：供 circle 服务分配/撤销成员角色、查询成员角色</li>
 * </ul>
 */
@Tag(name = "认证服务内部接口", description = "服务间 Feign 调用，不经网关")
@RestController
@RequestMapping("/auth/internal")
@RequiredArgsConstructor
public class AuthInternalController {

    private final CircleRoleService circleRoleService;

    // ---- 权限查询（AuthPermissionPort 契约，供各服务权限加载）----

    @GetMapping("/permissions/global-roles")
    public Result<List<String>> selectGlobalRoleCodes(@RequestParam("userId") Long userId) {
        return Result.success(circleRoleService.selectGlobalRoleCodes(userId));
    }

    @GetMapping("/permissions/global-permissions")
    public Result<List<String>> selectGlobalPermissionCodes(@RequestParam("userId") Long userId) {
        return Result.success(circleRoleService.selectGlobalPermissionCodes(userId));
    }

    @GetMapping("/permissions/circle-role-ids")
    public Result<List<Long>> selectCircleRoleIds(@RequestParam("userId") Long userId,
                                                  @RequestParam("circleId") Long circleId) {
        return Result.success(circleRoleService.selectUserRoleIdsInCircle(userId, circleId));
    }

    @PostMapping("/permissions/role-permissions")
    public Result<Set<String>> selectPermissionCodes(@RequestBody List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Result.success(Collections.emptySet());
        }
        return Result.success(circleRoleService.selectPermissionCodes(roleIds));
    }

    // ---- 圈子角色管理（供 circle 服务）----

    @PostMapping("/circle-roles/assign")
    public Result<Void> assignCircleRole(@RequestBody CircleRoleRequest request) {
        circleRoleService.assignRole(request.getUserId(), request.getRoleId(), request.getCircleId());
        return Result.success();
    }

    @PostMapping("/circle-roles/remove")
    public Result<Void> removeCircleRole(@RequestBody CircleRoleRequest request) {
        circleRoleService.removeRole(request.getUserId(), request.getRoleId(), request.getCircleId());
        return Result.success();
    }

    @GetMapping("/circle-roles/user-role-ids")
    public Result<List<Long>> selectUserRoleIdsInCircle(@RequestParam("userId") Long userId,
                                                        @RequestParam("circleId") Long circleId) {
        return Result.success(circleRoleService.selectUserRoleIdsInCircle(userId, circleId));
    }

    @GetMapping("/circle-roles/managed-ids")
    public Result<List<Long>> selectManagedCircleIds(@RequestParam("userId") Long userId) {
        return Result.success(circleRoleService.selectManagedCircleIds(userId));
    }

    @GetMapping("/circle-members")
    public Result<List<CircleMemberVO>> listCircleMembers(@RequestParam("circleId") Long circleId) {
        List<CircleMemberRoleVO> roles = circleRoleService.listCircleMembers(circleId);
        List<CircleMemberVO> vos = roles.stream().map(r -> {
            CircleMemberVO vo = new CircleMemberVO();
            vo.setUserId(r.getUserId());
            vo.setUsername(r.getUsername());
            vo.setRoleId(r.getRoleId());
            vo.setRoleCode(r.getRoleCode());
            vo.setRoleLabel(r.getRoleLabel());
            vo.setJoinTime(r.getJoinTime());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }
}
