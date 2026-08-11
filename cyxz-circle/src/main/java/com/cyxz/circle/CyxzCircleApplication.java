package com.cyxz.circle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 圈子服务启动类
 * <p>额外扫描 common.security.mapper（RBAC 跨库只读查询，供 @circlePerm 权限校验）。
 */
@MapperScan({"com.cyxz.circle.mapper", "com.cyxz.common.security.mapper"})
@SpringBootApplication(scanBasePackages = {"com.cyxz.circle", "com.cyxz.common"})
public class CyxzCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCircleApplication.class, args);
    }
}
