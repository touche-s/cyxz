package com.cyxz.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.auth.dto.UserRoleCode;
import com.cyxz.auth.entity.SysUserPO;
import com.cyxz.auth.mapper.SysUserMapper;
import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.vo.UserAdminVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 查询所有用户列表
     * <p>按创建时间倒序返回，排除 password 字段，角色从 sys_user_role 关联查询
     *
     * @return 用户管理 VO 列表（不含密码）
     */
    public List<UserAdminVO> listAll() {
        List<SysUserPO> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUserPO>()
                        .select(SysUserPO::getId, SysUserPO::getUsername,
                                SysUserPO::getStatus, SysUserPO::getCreateTime)
                        .orderByDesc(SysUserPO::getCreateTime)
        );
        // 批量查询全局角色映射，避免 N+1
        Map<Long, String> roleMap = sysUserRoleMapper.selectAllUserGlobalRoles().stream()
                .collect(Collectors.toMap(UserRoleCode::getUserId, UserRoleCode::getRoleCode, (a, b) -> a));
        return users.stream().map(po -> toVO(po, roleMap.getOrDefault(po.getId(), "USER"))).collect(Collectors.toList());
    }

    /**
     * 禁用指定用户
     * <p>保护站主：非站主本人不能禁用站主账号，避免 PLATFORM_ADMIN 通过禁用站主实现权限接管。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void disable(Long id) {
        SysUserPO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        guardSiteOwner(id);
        user.setStatus(0);
        sysUserMapper.updateById(user);
        log.info("管理员禁用用户: userId={}, username={}", id, user.getUsername());
    }

    /**
     * 启用指定用户
     * <p>同样受 {@link #guardSiteOwner} 保护，保持与 disable 一致。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void enable(Long id) {
        SysUserPO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        guardSiteOwner(id);
        user.setStatus(1);
        sysUserMapper.updateById(user);
        log.info("管理员启用用户: userId={}, username={}", id, user.getUsername());
    }

    /**
     * 站主保护：目标用户是 SITE_OWNER 时，仅当前用户也是 SITE_OWNER 才放行
     */
    private void guardSiteOwner(Long targetUserId) {
        Set<String> targetRoles = sysUserRoleMapper.selectGlobalRoleCodes(targetUserId)
                .stream().collect(Collectors.toSet());
        if (targetRoles.contains("SITE_OWNER") && !SecurityUtils.currentRoles().contains("SITE_OWNER")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作站主账号");
        }
    }

    private UserAdminVO toVO(SysUserPO po, String roleCode) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(po.getId());
        vo.setUsername(po.getUsername());
        vo.setRole(roleCode);
        vo.setStatus(po.getStatus());
        return vo;
    }
}
