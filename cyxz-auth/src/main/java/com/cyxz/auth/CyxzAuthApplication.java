package com.cyxz.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cyxz.auth", "com.cyxz.common"})
public class CyxzAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzAuthApplication.class, args);
    }
}
