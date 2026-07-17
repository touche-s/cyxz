package com.cyxz.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 上传服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cyxz.upload", "com.cyxz.common"})
public class CyxzUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzUploadApplication.class, args);
    }
}
