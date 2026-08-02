package com.cyxz.post;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.cyxz.post.mapper")
@SpringBootApplication(scanBasePackages = {"com.cyxz.post", "com.cyxz.common", "com.cyxz.user.feign", "com.cyxz.comment.feign", "com.cyxz.message.feign", "com.cyxz.circle.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.user.feign", "com.cyxz.comment.feign", "com.cyxz.message.feign", "com.cyxz.circle.feign"})
@EnableScheduling
public class CyxzPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzPostApplication.class, args);
    }
}
