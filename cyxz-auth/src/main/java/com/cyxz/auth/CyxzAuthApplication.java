package com.cyxz.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 */
@MapperScan({"com.cyxz.auth.mapper", "com.cyxz.common.security.mapper"})
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
@SpringBootApplication(scanBasePackages = {"com.cyxz.auth", "com.cyxz.common", "com.cyxz.user.feign"})
public class CyxzAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAuthApplication.class, args);
    }
}
