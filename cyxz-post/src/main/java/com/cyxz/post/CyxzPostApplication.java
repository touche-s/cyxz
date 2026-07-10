package com.cyxz.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.cyxz.post", "com.cyxz.common"})
@EnableFeignClients(basePackages = "com.cyxz.user.feign")
public class CyxzPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzPostApplication.class, args);
    }
}
