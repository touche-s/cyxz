package com.cyxz.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cyxz.message", "com.cyxz.common", "com.cyxz.auth"})
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
@EnableScheduling
public class CyxzMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzMessageApplication.class, args);
    }
}
