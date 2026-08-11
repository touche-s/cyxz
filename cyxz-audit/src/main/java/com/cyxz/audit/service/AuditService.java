package com.cyxz.audit.service;

import com.cyxz.audit.vo.AuditLogVO;
import com.cyxz.common.base.PageResult;

import java.time.LocalDate;

/**
 * 审计日志服务
 */
public interface AuditService {

    /**
     * 审计日志列表（按动作/对象类型/操作人/时间范围筛选，分页）
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
    PageResult<AuditLogVO> list(String action, String targetType, Long operatorId,
                                LocalDate startDate, LocalDate endDate, int page, int size);
}
