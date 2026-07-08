package com.cyxz.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cyxz.post", "com.cyxz.common"})
public class CyxzPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzPostApplication.class, args);
    }
}
