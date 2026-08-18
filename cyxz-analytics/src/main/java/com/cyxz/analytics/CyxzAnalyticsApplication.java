package com.cyxz.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数据统计服务启动类
 * <p>承载全站业务指标聚合：消费各服务发布的统计事件并落库到每日统计表，对外提供看板与趋势查询。
 * <p>开启 Feign（auth.feign），供 @PreAuthorize 权限校验经 {@code AuthFeignClient} 查询全局权限。
 */
@MapperScan("com.cyxz.analytics.mapper")
@SpringBootApplication(scanBasePackages = {"com.cyxz.analytics", "com.cyxz.common", "com.cyxz.auth.feign"})
@EnableFeignClients(basePackages = "com.cyxz.auth.feign")
public class CyxzAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAnalyticsApplication.class, args);
    }
}
