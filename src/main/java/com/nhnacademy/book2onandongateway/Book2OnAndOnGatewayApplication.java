package com.nhnacademy.book2onandongateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class Book2OnAndOnGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(Book2OnAndOnGatewayApplication.class, args);
    }

}
