package com.cyxz.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 */
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
@SpringBootApplication(scanBasePackages = {"com.cyxz.auth", "com.cyxz.common"})
public class CyxzAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAuthApplication.class, args);
    }
}
