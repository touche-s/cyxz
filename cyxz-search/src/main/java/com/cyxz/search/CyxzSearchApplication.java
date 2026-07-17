package com.cyxz.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cyxz.search", "com.cyxz.common"})
public class CyxzSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzSearchApplication.class, args);
    }
}
