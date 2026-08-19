package com.cyxz.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.auth.dto.UserRoleCode;
import com.cyxz.auth.entity.SysUserPO;
import com.cyxz.auth.mapper.SysUserMapper;
import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.vo.UserAdminVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询用户列表
     * <p>按创建时间倒序，排除 password 字段，角色从 sys_user_role 关联查询
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 分页用户管理 VO 列表（不含密码）
     */
    public PageResult<UserAdminVO> listAll(int page, int size) {
        Page<SysUserPO> pageResult = sysUserMapper.selectPage(
                PageConstants.pageOf(page, size),
                new LambdaQueryWrapper<SysUserPO>()
                        .select(SysUserPO::getId, SysUserPO::getUsername,
                                SysUserPO::getStatus, SysUserPO::getCreateTime)
                        .orderByDesc(SysUserPO::getCreateTime)
        );
        // 批量查询全局角色映射，避免 N+1
        Map<Long, String> roleMap = sysUserRoleMapper.selectAllUserGlobalRoles().stream()
                .collect(Collectors.toMap(UserRoleCode::getUserId, UserRoleCode::getRoleCode, (a, b) -> a));
        List<UserAdminVO> voList = pageResult.getRecords().stream()
                .map(po -> toVO(po, roleMap.getOrDefault(po.getId(), "USER")))
                .collect(Collectors.toList());
        return PageResult.of(voList, pageResult.getTotal(), page, size);
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
        user.setStatus(CommonStatus.DELETED);
        sysUserMapper.updateById(user);
        // 发布审计事件：用户封禁
        Long operatorId = SecurityUtils.currentUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 写入禁用标记，网关实时拦截其未过期 JWT；TTL 覆盖 Token 最长有效期
                    stringRedisTemplate.opsForValue().set(CacheKeyConstants.getUserDisabledKey(id), "1",
                            CacheKeyConstants.USER_DISABLED_TTL_DAYS, TimeUnit.DAYS);
                } catch (Exception e) {
                    log.error("写入用户禁用标记失败，禁用可能不即时生效: userId={}", id, e);
                }
                try {
                    AuditEvent auditEvent = AuditEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .operatorId(operatorId)
                            .operatorName(null)
                            .action(AuditConstants.ACTION_USER_DISABLE)
                            .targetType("USER")
                            .targetId(id)
                            .detail(null)
                            .ip(null)
                            .createTime(LocalDateTime.now())
                            .build();
                    rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
                } catch (Exception e) {
                    log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_USER_DISABLE, id, e);
                }
            }
        });
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
        user.setStatus(CommonStatus.ACTIVE);
        sysUserMapper.updateById(user);
        // 发布审计事件：用户解禁
        Long operatorId = SecurityUtils.currentUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 解禁后移除禁用标记，恢复访问
                    stringRedisTemplate.delete(CacheKeyConstants.getUserDisabledKey(id));
                } catch (Exception e) {
                    log.error("删除用户禁用标记失败: userId={}", id, e);
                }
                try {
                    AuditEvent auditEvent = AuditEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .operatorId(operatorId)
                            .operatorName(null)
                            .action(AuditConstants.ACTION_USER_ENABLE)
                            .targetType("USER")
                            .targetId(id)
                            .detail(null)
                            .ip(null)
                            .createTime(LocalDateTime.now())
                            .build();
                    rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
                } catch (Exception e) {
                    log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_USER_ENABLE, id, e);
                }
            }
        });
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
