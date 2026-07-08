package com.cyxz.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cyxz.user", "com.cyxz.common"})
public class CyxzUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzUserApplication.class, args);
    }
}
