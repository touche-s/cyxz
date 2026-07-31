package com.cyxz.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cyxz.comment", "com.cyxz.common", "com.cyxz.user.feign", "com.cyxz.post.feign", "com.cyxz.message.api.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.user.feign", "com.cyxz.post.feign", "com.cyxz.message.api.feign"})
@EnableScheduling
public class CyxzCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCommentApplication.class, args);
    }
}
