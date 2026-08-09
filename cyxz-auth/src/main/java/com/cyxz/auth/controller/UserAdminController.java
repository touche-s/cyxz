package com.cyxz.auth.controller;

import com.cyxz.auth.service.UserAdminService;
import com.cyxz.auth.vo.UserAdminVO;
import com.cyxz.common.base.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理（管理员端）
 * <p>权限校验由 {@code @PreAuthorize} 基于 JWT 下发的全局权限码完成，
 * SecurityConfig 的 AdminRoleFilter 对 /auth/admin/** 路径做 X-User-Role 纵深防护。
 */
@Tag(name = "用户管理", description = "用户管理（管理员端）")
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    /**
     * 查询全部用户列表
     *
     * @return 用户管理视图对象列表
     */
    @Operation(summary = "查询全部用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:manage:list')")
    public Result<List<UserAdminVO>> list() {
        return Result.success(userAdminService.listAll());
    }

    /**
     * 禁用指定用户
     *
     * @param id 被禁用用户的唯一标识
     * @return 无业务数据的统一响应
     */
    @Operation(summary = "禁用指定用户")
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('user:manage:disable')")
    public Result<Void> disable(@PathVariable Long id) {
        userAdminService.disable(id);
        return Result.success();
    }

    /**
     * 启用指定用户
     *
     * @param id 被启用用户的唯一标识
     * @return 无业务数据的统一响应
     */
    @Operation(summary = "启用指定用户")
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('user:manage:enable')")
    public Result<Void> enable(@PathVariable Long id) {
        userAdminService.enable(id);
        return Result.success();
    }
}
