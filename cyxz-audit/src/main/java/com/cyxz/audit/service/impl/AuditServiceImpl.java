package com.cyxz.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.entity.AuditLogPO;
import com.cyxz.audit.mapper.AuditLogMapper;
import com.cyxz.audit.service.AuditService;
import com.cyxz.audit.vo.AuditLogVO;
import com.cyxz.common.base.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 审计日志服务实现
 * <p>提供管理端审计日志的多条件分页查询，按创建时间倒序返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 审计日志列表（按动作/对象类型/操作人/时间范围筛选，分页）
     * <p>时间范围按天闭区间：startDate 取当天 00:00:00 起，endDate 取当天 23:59:59.999999999 止。
     *
     * @param action     动作筛选（null=全部）
     * @param targetType 对象类型筛选（null=全部）
     * @param operatorId 操作人用户 ID 筛选（null=全部）
     * @param startDate  起始日期（含，null=不限）
     * @param endDate    结束日期（含，null=不限）
     * @param page       页码
     * @param size       每页条数
     * @return 审计日志分页列表
     */
    @Override
    public PageResult<AuditLogVO> list(String action, String targetType, Long operatorId,
                                       LocalDate startDate, LocalDate endDate, int page, int size) {
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<AuditLogPO>()
                .eq(StringUtils.hasText(action), AuditLogPO::getAction, action)
                .eq(StringUtils.hasText(targetType), AuditLogPO::getTargetType, targetType)
                .eq(operatorId != null, AuditLogPO::getOperatorId, operatorId)
                .ge(startDate != null, AuditLogPO::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, AuditLogPO::getCreateTime, endDate == null ? null : endDate.atTime(LocalTime.MAX))
                .orderByDesc(AuditLogPO::getCreateTime);
        Page<AuditLogPO> p = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    /**
     * PO 转 VO
     *
     * @param po 审计日志实体
     * @return 审计日志 VO
     */
    private AuditLogVO toVO(AuditLogPO po) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
