package com.cyxz.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.cyxz.message", "com.cyxz.common"})
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
public class CyxzMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzMessageApplication.class, args);
    }
}
