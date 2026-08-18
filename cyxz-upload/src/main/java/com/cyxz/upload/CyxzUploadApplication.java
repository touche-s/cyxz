package com.cyxz.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 上传服务启动类
 * <p>纯 MinIO 存储服务，无数据库访问。
 * <p>开启 Feign（com.cyxz.auth.feign / com.cyxz.circle.feign），供 @PreAuthorize 权限校验经
 * {@code AuthFeignClient}/{@code CircleFeignClient} 查询全局与圈子权限。
 */
@SpringBootApplication(scanBasePackages = {"com.cyxz.upload", "com.cyxz.common", "com.cyxz.auth.feign", "com.cyxz.circle.feign"})
@EnableFeignClients(basePackages = {"com.cyxz.auth.feign", "com.cyxz.circle.feign"})
public class CyxzUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzUploadApplication.class, args);
    }
}
