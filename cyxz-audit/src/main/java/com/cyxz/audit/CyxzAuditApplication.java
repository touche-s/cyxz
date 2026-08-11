package com.cyxz.audit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 审计日志中心启动类
 * <p>承载审计日志的异步落库与管理端查询能力：消费各业务服务发布的 {@code AuditEvent} 写入 audit_log 表，
 * 并提供平台管理员的审计日志检索接口。
 * <p>额外扫描 common.security.mapper（RBAC 跨库只读查询，供 @PreAuthorize 权限校验）。
 */
@MapperScan({"com.cyxz.audit.mapper", "com.cyxz.common.security.mapper"})
@SpringBootApplication(scanBasePackages = {"com.cyxz.audit", "com.cyxz.common"})
public class CyxzAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAuditApplication.class, args);
    }
}
