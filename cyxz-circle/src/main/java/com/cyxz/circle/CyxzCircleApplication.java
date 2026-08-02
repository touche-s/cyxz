package com.cyxz.circle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 圈子服务启动类
 */
@MapperScan("com.cyxz.circle.mapper")
@SpringBootApplication(scanBasePackages = {"com.cyxz.circle", "com.cyxz.common"})
@EnableFeignClients(basePackages = "com.cyxz.post.feign")
@EnableScheduling
public class CyxzCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCircleApplication.class, args);
    }
}
