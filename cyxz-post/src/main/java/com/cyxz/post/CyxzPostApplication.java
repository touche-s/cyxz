package com.cyxz.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cyxz.post", "com.cyxz.common", "com.cyxz.user.service", "com.cyxz.user.feign"})
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
@EnableScheduling
public class CyxzPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzPostApplication.class, args);
    }
}
