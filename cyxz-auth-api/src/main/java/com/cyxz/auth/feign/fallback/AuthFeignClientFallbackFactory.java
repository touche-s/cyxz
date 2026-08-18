package com.cyxz.auth.feign.fallback;

import com.cyxz.auth.feign.AuthFeignClient;
import com.cyxz.auth.feign.dto.CircleRoleRequest;
import com.cyxz.auth.feign.vo.CircleMemberVO;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 认证服务 Feign 降级工厂
 * <p>查询类接口返回安全默认值（空集合，配合 fail-closed 由调用方决定行为）；
 * 写操作（圈子角色分配/撤销）降级返回失败结果，调用方需检查 {@link Result#isSuccess()}，
 * 避免静默丢失角色变更导致权限错乱。
 * <p>使用 {@code @ConditionalOnClass} 确保仅在有 openfeign 的消费方服务中注册。
 */
@Component
@ConditionalOnClass(FallbackFactory.class)
public class AuthFeignClientFallbackFactory extends AbstractFeignFallbackFactory<AuthFeignClient> {

    @Override
    protected String serviceName() {
        return "认证服务";
    }

    @Override
    protected AuthFeignClient createFallback(Throwable cause) {
        return new AuthFeignClient() {
            @Override
            public Result<List<String>> selectGlobalRoleCodes(Long userId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<String>> selectGlobalPermissionCodes(Long userId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<Long>> selectCircleRoleIds(Long userId, Long circleId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Set<String>> selectPermissionCodes(List<Long> roleIds) {
                return Result.success(Collections.emptySet());
            }

            @Override
            public Result<Void> assignCircleRole(CircleRoleRequest request) {
                return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMsg());
            }

            @Override
            public Result<Void> removeCircleRole(CircleRoleRequest request) {
                return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMsg());
            }

            @Override
            public Result<List<Long>> selectUserRoleIdsInCircle(Long userId, Long circleId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<Long>> selectManagedCircleIds(Long userId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<CircleMemberVO>> listCircleMembers(Long circleId) {
                return Result.success(Collections.emptyList());
            }
        };
    }
}
