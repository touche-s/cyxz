package com.cyxz.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CyxzGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyxzGatewayApplication.class, args);
    }
}
