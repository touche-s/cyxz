package com.cyxz.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.cyxz.user", "com.cyxz.common", "com.cyxz.message.api.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.message.api.feign"})
public class CyxzUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzUserApplication.class, args);
    }
}
