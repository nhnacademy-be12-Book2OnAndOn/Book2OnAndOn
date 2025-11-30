package com.nhnacademy.book2onandongateway.config;

import com.nhnacademy.book2onandongateway.filter.AuthorizationHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {
    private final AuthorizationHeaderFilter authFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){

        return builder.routes() //예시일뿐 수정해야함
                //BookService
                .route("book-service-route",
                        r -> r.path("/api/books/**", "/api/admin/books/**")
                                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                                .uri("lb://BOOK-SERVICE"))
                //OrderService
                .route("order-service-route",
                        r -> r.path("/api/orders/**", "/api/admin/orders/**")
                                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                                .uri("lb://ORDER-SERVICE"))
                //UserService
                // 접근 권한(전부)
                .route("user-service-auth", r -> r.path("/api/auth/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                        .uri("lb://USER-SERVICE"))
                .route("user-service-public-reviews", r -> r.path("/api/users/*/reviews")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}")) // 필터 없이 Pass!
                        .uri("lb://USER-SERVICE"))

                // 권한 검사
                .route("user-service-user", r -> r.path("/api/users/**", "/api/grades/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}")
                                // Config를 빈 상태로 넘기면 토큰 유효성만 검사해서 로그인 여부 확인
                                .filter(authFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://USER-SERVICE"))

                .route("user-service-admin", r -> r.path("/api/admin/users/**", "/api/admin/grades/**")
                        .filters(f -> {
                            AuthorizationHeaderFilter.Config config = new AuthorizationHeaderFilter.Config();
                            config.setRole("ROLE_MEMBER_ADMIN"); //회원 관리자만 통과하도록 

                            return f.rewritePath("/api/(?<segment>.*)", "/${segment}")
                                    .filter(authFilter.apply(config));
                        })
                        .uri("lb://USER-SERVICE"))

                //CouponService
                .route("coupon-service-route",
                        r-> r.path("/api/coupons/**", "/api/admin/coupons/**")
                                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                                .uri("lb://COUPON-SERVICE"))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 쿠키/인증정보 포함 허용
        config.addAllowedOriginPattern("*"); // 모든 도메인 허용 (운영 시엔 프론트 도메인 적어야됨!!!!!1)
        config.addAllowedHeader("*");
        config.addAllowedMethod("*"); // GET, POST, DELETE , PUT ... 모든 방식 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}

/*
Nginx -> Front -> Gateway -> Service

Nginx: /api/books/1 요청을 받으면 Gateway로 토스합니다 (/api/)경로는 Gateway로 Nginx에 지정함
Gateway: 1. 요청 주소가 /api/books/1 이므로 .path에 매칭됩니다.
         2. 필터링이 작동합니다. (정규식에 의해 /api/ 뒤에 books/1을 segment 변수에 담아 주소를 /books/1로 바꿔버립니다.
         3. lb://BOOK-SERVICE를 찾아가서 /books/1을 요청합니다.
서비스로직: /books/1 만 받아서 로직을 처리합니다.
 */

/*
CorsWebFilter 란?
한마디로 정의하자면 "브라우저 보안 검문소 프리패스권"이다.
웹 브라우저가 보안상 주소(도메인/포트)가 다르면 JS로 데이터를 요청하는 걸 기본적으로 차단! (SOP 정책)

이 필터의 역할
- Front에서 요청이오면 브라우저가 (나 프론트 포트에서 왔는데 게이트웨이 포트 데이터를 가져가도돼?) 라고 물어봄
- 그럼 이 필터는 우리 서버는 다 허용해~ 라고 응답 헤더를 붙임
- 브라우저는 안심하고 실제 데이터를 요청
 */


