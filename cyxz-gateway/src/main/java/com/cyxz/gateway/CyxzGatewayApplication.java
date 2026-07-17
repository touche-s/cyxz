package com.cyxz.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cyxz.gateway", "com.cyxz.common", "com.cyxz.auth"})
public class CyxzGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzGatewayApplication.class, args);
    }
}
