package com.cyxz.circle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 圈子服务启动类
 * <p>RBAC 圈子角色归属 auth 服务，经 {@code AuthFeignClient}（Feign）调用；
 * 成员昵称/头像经 {@code UserFeignClient} 调用 user 服务。
 */
@MapperScan("com.cyxz.circle.mapper")
@EnableFeignClients(basePackages = {"com.cyxz.auth.feign", "com.cyxz.user.feign"})
@SpringBootApplication(scanBasePackages = {"com.cyxz.circle", "com.cyxz.common", "com.cyxz.auth.feign", "com.cyxz.user.feign"})
public class CyxzCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzCircleApplication.class, args);
    }
}
