package com.cyxz.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据统计服务启动类
 * <p>承载全站业务指标聚合：消费各服务发布的统计事件并落库到每日统计表，对外提供看板与趋势查询。
 * <p>额外扫描 common.security.mapper（RBAC 跨库只读查询，供 @PreAuthorize 权限校验）。
 */
@MapperScan({"com.cyxz.analytics.mapper", "com.cyxz.common.security.mapper"})
@SpringBootApplication(scanBasePackages = {"com.cyxz.analytics", "com.cyxz.common"})
public class CyxzAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAnalyticsApplication.class, args);
    }
}
