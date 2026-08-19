package com.cyxz.audit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 审计日志中心启动类
 * <p>承载审计日志的异步落库与管理端查询能力：消费各业务服务发布的 {@code AuditEvent} 写入 audit_log 表，
 * 并提供平台管理员的审计日志检索接口。
 * <p>开启 Feign（auth.feign），供 @PreAuthorize 权限校验经 {@code AuthFeignClient} 查询全局权限。
 */
@MapperScan("com.cyxz.audit.mapper")
@SpringBootApplication(scanBasePackages = {"com.cyxz.audit", "com.cyxz.common", "com.cyxz.auth.feign"})
@EnableFeignClients(basePackages = "com.cyxz.auth.feign")
public class CyxzAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAuditApplication.class, args);
    }
}
