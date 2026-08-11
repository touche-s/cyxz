package com.cyxz.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.auth.entity.SysPermissionPO;
import com.cyxz.auth.entity.SysRolePO;
import com.cyxz.auth.entity.SysRolePermissionPO;
import com.cyxz.auth.entity.SysUserRolePO;
import com.cyxz.auth.mapper.SysPermissionMapper;
import com.cyxz.auth.mapper.SysRoleMapper;
import com.cyxz.auth.mapper.SysRolePermissionMapper;
import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.vo.PermissionVO;
import com.cyxz.auth.vo.RoleVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.security.SecurityUtils;
import com.cyxz.common.utils.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 管理服务
 * <p>提供角色列表、权限点列表、角色-权限分配、用户全局角色分配等管理能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAdminService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询全部角色列表（按 sort 排序）
     */
    public List<RoleVO> listRoles() {
        List<SysRolePO> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRolePO>().orderByAsc(SysRolePO::getSort)
        );
        return roles.stream().map(this::toRoleVO).collect(Collectors.toList());
    }

    /**
     * 查询全部权限点列表
     */
    public List<PermissionVO> listPermissions() {
        List<SysPermissionPO> perms = sysPermissionMapper.selectList(null);
        return perms.stream().map(this::toPermissionVO).collect(Collectors.toList());
    }

    /**
     * 查询角色已分配的权限 ID 列表
     */
    public List<Long> getRolePermissionIds(Long roleId) {
        return sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 更新角色的权限分配（先删后插）
     * <p>内置角色（{@code built_in=1}，如 SITE_OWNER/PLATFORM_ADMIN/USER）禁止修改权限分配，
     * 防止清空站主权限或给普通用户角色塞管理员权限等权限提升攻击。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表（全量覆盖）
     */
    @Transactional
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        SysRolePO role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "内置角色权限不允许修改");
        }
        // 先删除旧关联
        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermissionPO>().eq(SysRolePermissionPO::getRoleId, roleId)
        );
        // 再批量插入新关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                SysRolePermissionPO rp = new SysRolePermissionPO();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        log.info("更新角色权限: roleId={}, roleCode={}, permissionCount={}", roleId, role.getCode(),
                permissionIds != null ? permissionIds.size() : 0);

        // Cache-Aside 旁路失效：查 DB 找出受影响用户 → 逐个删 key
        invalidateRolePermissionCache(roleId);
    }

    /**
     * 角色权限配置变更后，逐个删除受影响用户的权限缓存
     * <p>缓存删除放到事务提交后执行，避免 T1 删缓存→T2 读旧值回写→T1 提交导致的缓存-DB 不一致。
     */
    private void invalidateRolePermissionCache(Long roleId) {
        TransactionUtils.afterCommit(() -> {
            try {
                List<SysUserRolePO> entries = sysUserRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRolePO>().eq(SysUserRolePO::getRoleId, roleId)
                );
                for (SysUserRolePO entry : entries) {
                    if (entry.getCircleId() != null && entry.getCircleId() == 0L) {
                        stringRedisTemplate.delete(CacheKeyConstants.getAuthGlobalKey(entry.getUserId()));
                    } else if (entry.getCircleId() != null) {
                        stringRedisTemplate.delete(CacheKeyConstants.getAuthCircleKey(entry.getUserId(), entry.getCircleId()));
                    }
                }
                log.info("角色权限缓存失效完成: roleId={}, affectedUsers={}", roleId, entries.size());
            } catch (Exception e) {
                log.warn("角色权限缓存失效失败，等待 TTL 自然过期: roleId={}", roleId, e);
            }
        });
    }

    /**
     * 分配用户的全局角色（覆盖式：先删旧全局角色再插新的）
     * <p>SITE_OWNER 角色只能由站主本人分配，避免 PLATFORM_ADMIN 自行提权或剥夺站主权限。
     *
     * @param userId 目标用户 ID
     * @param roleId 角色 ID（必须是 GLOBAL scope）
     */
    @Transactional
    public void assignUserGlobalRole(Long userId, Long roleId) {
        SysRolePO role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (!"GLOBAL".equals(role.getScope())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能分配全局角色");
        }
        if ("SITE_OWNER".equals(role.getCode()) && !SecurityUtils.currentRoles().contains("SITE_OWNER")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有站主可以分配站主角色");
        }
        // 删除用户当前的全局角色关联（circle_id=0）
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRolePO>()
                        .eq(SysUserRolePO::getUserId, userId)
                        .eq(SysUserRolePO::getCircleId, 0L)
        );
        // 插入新的全局角色关联
        SysUserRolePO ur = new SysUserRolePO();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setCircleId(0L);
        sysUserRoleMapper.insert(ur);
        // 清除该用户的全局权限缓存（放 afterCommit，避免事务回滚后缓存已被清空）
        TransactionUtils.afterCommit(() ->
                stringRedisTemplate.delete(CacheKeyConstants.getAuthGlobalKey(userId)));
        log.info("分配用户全局角色: userId={}, roleId={}, roleCode={}", userId, roleId, role.getCode());
    }

    private RoleVO toRoleVO(SysRolePO po) {
        RoleVO vo = new RoleVO();
        vo.setId(po.getId());
        vo.setCode(po.getCode());
        vo.setLabel(po.getLabel());
        vo.setScope(po.getScope());
        vo.setDescription(po.getDescription());
        vo.setBuiltIn(po.getBuiltIn());
        vo.setSort(po.getSort());
        return vo;
    }

    private PermissionVO toPermissionVO(SysPermissionPO po) {
        PermissionVO vo = new PermissionVO();
        vo.setId(po.getId());
        vo.setCode(po.getCode());
        vo.setLabel(po.getLabel());
        vo.setResource(po.getResource());
        vo.setAction(po.getAction());
        return vo;
    }
}
