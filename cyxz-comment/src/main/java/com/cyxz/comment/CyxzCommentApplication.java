package com.cyxz.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.cyxz.comment.mapper")
@SpringBootApplication(scanBasePackages = {"com.cyxz.comment", "com.cyxz.common", "com.cyxz.user.feign", "com.cyxz.post.feign", "com.cyxz.circle.feign", "com.cyxz.message.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.user.feign", "com.cyxz.post.feign", "com.cyxz.circle.feign", "com.cyxz.message.feign"})
@EnableScheduling
public class CyxzCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCommentApplication.class, args);
    }
}
