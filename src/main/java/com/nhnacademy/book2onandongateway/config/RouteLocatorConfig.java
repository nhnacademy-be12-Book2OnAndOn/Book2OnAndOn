package com.nhnacademy.book2onandongateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        return builder.routes() //예시일뿐 수정해야함
                .route("book-service-route",
                        r -> r.path("/books/**", "/admin/books/**")
                                .uri("lb://BOOK-SERVICE"))
                .route("order-service-route",
                        r -> r.path("/orders/**", "/admin/orders/**")
                                .uri("lb://ORDER-SERVICE"))
                .route("user-service-route",
                        r-> r.path("/users/**", "/admin/users/**")
                                .uri("lb://USER-SERVICE"))
                .route("coupon-service-route",
                        r-> r.path("/coupons/**", "/admin/coupons/**")
                                .uri("lb://COUPON-SERVICE"))
                .build();
    }
}
