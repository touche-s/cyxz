package com.cyxz.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.cyxz.comment", "com.cyxz.common", "com.cyxz.user.service", "com.cyxz.user.feign", "com.cyxz.post.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.user.feign", "com.cyxz.post.feign"})
public class CyxzCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCommentApplication.class, args);
    }
}
