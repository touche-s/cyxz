package com.cyxz.auth.feign;

import com.cyxz.auth.feign.dto.CircleRoleRequest;
import com.cyxz.auth.feign.fallback.AuthFeignClientFallbackFactory;
import com.cyxz.auth.feign.vo.CircleMemberVO;
import com.cyxz.common.base.Result;
import com.cyxz.common.security.AuthPermissionPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 认证服务 Feign 客户端
 * <p>继承 {@link AuthPermissionPort} 提供 RBAC 权限查询；另提供圈子角色管理与圈子成员角色查询。
 * <p>供 circle（角色分配/撤销/成员查询）与其他服务（权限加载）调用。
 */
@FeignClient(name = "cyxz-auth", fallbackFactory = AuthFeignClientFallbackFactory.class)
public interface AuthFeignClient extends AuthPermissionPort {

    /**
     * 分配圈子角色（INSERT IGNORE 幂等）
     */
    @PostMapping("/auth/internal/circle-roles/assign")
    Result<Void> assignCircleRole(@RequestBody CircleRoleRequest request);

    /**
     * 撤销圈子角色（幂等）
     */
    @PostMapping("/auth/internal/circle-roles/remove")
    Result<Void> removeCircleRole(@RequestBody CircleRoleRequest request);

    /**
     * 查询用户在圈子中的角色 ID 列表
     */
    @GetMapping("/auth/internal/circle-roles/user-role-ids")
    Result<List<Long>> selectUserRoleIdsInCircle(@RequestParam("userId") Long userId,
                                                 @RequestParam("circleId") Long circleId);

    /**
     * 查询用户管理的圈子 ID 列表（圈主或圈子管理员）
     */
    @GetMapping("/auth/internal/circle-roles/managed-ids")
    Result<List<Long>> selectManagedCircleIds(@RequestParam("userId") Long userId);

    /**
     * 查询圈子成员的角色信息列表（按圈主→管理员→成员排序）
     */
    @GetMapping("/auth/internal/circle-members")
    Result<List<CircleMemberVO>> listCircleMembers(@RequestParam("circleId") Long circleId);
}
