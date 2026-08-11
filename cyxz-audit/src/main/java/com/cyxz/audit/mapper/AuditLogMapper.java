package com.cyxz.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.audit.entity.AuditLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogPO> {
}
