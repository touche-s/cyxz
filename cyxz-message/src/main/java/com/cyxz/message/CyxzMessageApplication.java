package com.cyxz.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cyxz.message", "com.cyxz.common"})
public class CyxzMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzMessageApplication.class, args);
    }
}
