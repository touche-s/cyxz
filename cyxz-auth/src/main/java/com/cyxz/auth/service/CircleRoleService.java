package com.cyxz.auth.service;

import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.vo.CircleMemberRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 圈子角色管理服务
 * <p>承载 sys_user_role 圈子维度（circle_id &gt; 0）的分配/撤销与查询，
 * 由 auth 服务持有（其表 owner），circle 服务经 {@code AuthFeignClient} 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleRoleService {

    /** 圈主角色 ID（sys_role.id=4） */
    public static final long CIRCLE_OWNER_ROLE_ID = 4L;
    /** 圈子管理员角色 ID（sys_role.id=5） */
    public static final long CIRCLE_ADMIN_ROLE_ID = 5L;
    /** 圈子成员角色 ID（sys_role.id=6） */
    public static final long CIRCLE_MEMBER_ROLE_ID = 6L;

    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 查询用户的全局角色 code 列表（circle_id=0）
     */
    public List<String> selectGlobalRoleCodes(Long userId) {
        return sysUserRoleMapper.selectGlobalRoleCodes(userId);
    }

    /**
     * 查询用户的全局权限码列表（circle_id=0 的全局角色关联的权限）
     */
    public List<String> selectGlobalPermissionCodes(Long userId) {
        return sysUserRoleMapper.selectGlobalPermissionCodes(userId);
    }

    /**
     * 分配圈子角色（INSERT IGNORE 幂等）
     */
    public void assignRole(Long userId, Long roleId, Long circleId) {
        sysUserRoleMapper.assignRole(userId, roleId, circleId);
    }

    /**
     * 撤销圈子角色（幂等）
     */
    public void removeRole(Long userId, Long roleId, Long circleId) {
        sysUserRoleMapper.removeRole(userId, roleId, circleId);
    }

    /**
     * 查询用户在圈子中的角色 ID 列表
     */
    public List<Long> selectUserRoleIdsInCircle(Long userId, Long circleId) {
        return sysUserRoleMapper.selectCircleRoleIds(userId, circleId);
    }

    /**
     * 查询用户管理的圈子 ID 列表（圈主或圈子管理员）
     */
    public List<Long> selectManagedCircleIds(Long userId) {
        return sysUserRoleMapper.selectManagedCircleIds(userId,
                CIRCLE_OWNER_ROLE_ID, CIRCLE_ADMIN_ROLE_ID);
    }

    /**
     * 查询圈子成员的角色信息列表（按圈主→管理员→成员排序）
     */
    public List<CircleMemberRoleVO> listCircleMembers(Long circleId) {
        return sysUserRoleMapper.selectCircleMembers(circleId,
                CIRCLE_OWNER_ROLE_ID, CIRCLE_ADMIN_ROLE_ID, CIRCLE_MEMBER_ROLE_ID);
    }

    /**
     * 根据角色 ID 集合查询其拥有的权限码集合
     */
    public Set<String> selectPermissionCodes(List<Long> roleIds) {
        return sysUserRoleMapper.selectPermissionCodes(roleIds);
    }
}
