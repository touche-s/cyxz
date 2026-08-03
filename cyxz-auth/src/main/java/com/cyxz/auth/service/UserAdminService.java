package com.cyxz.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.auth.entity.SysUserPO;
import com.cyxz.auth.mapper.SysUserMapper;
import com.cyxz.auth.vo.UserAdminVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final SysUserMapper sysUserMapper;

    public List<UserAdminVO> listAll() {
        // 排除 password 字段，避免无谓加载 BCrypt 哈希
        List<SysUserPO> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUserPO>()
                        .select(SysUserPO::getId, SysUserPO::getUsername,
                                SysUserPO::getRole, SysUserPO::getStatus,
                                SysUserPO::getCreateTime)
                        .orderByDesc(SysUserPO::getCreateTime)
        );
        return users.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public void disable(Long id) {
        SysUserPO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
        log.info("管理员禁用用户: userId={}, username={}", id, user.getUsername());
    }

    @Transactional
    public void enable(Long id) {
        SysUserPO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(1);
        sysUserMapper.updateById(user);
        log.info("管理员启用用户: userId={}, username={}", id, user.getUsername());
    }

    private UserAdminVO toVO(SysUserPO po) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(po.getId());
        vo.setUsername(po.getUsername());
        vo.setRole(po.getRole());
        vo.setStatus(po.getStatus());
        return vo;
    }
}
