package com.cyxz.governance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 内容治理中心启动类
 * <p>承载举报处理、内容处置事件发布等治理能力。
 * <p>额外扫描 common.security.mapper（RBAC 跨库只读查询，供 @PreAuthorize 权限校验）。
 */
@MapperScan({"com.cyxz.governance.mapper", "com.cyxz.common.security.mapper"})
@SpringBootApplication(scanBasePackages = {"com.cyxz.governance", "com.cyxz.common"})
public class CyxzGovernanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzGovernanceApplication.class, args);
    }
}
