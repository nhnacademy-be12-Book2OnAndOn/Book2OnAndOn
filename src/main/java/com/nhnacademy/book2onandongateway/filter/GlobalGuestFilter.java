package com.nhnacademy.book2onandongateway.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalGuestFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String guestId = null;
        HttpCookie cookie = request.getCookies().getFirst("GUEST_ID");

        boolean isNewGuest = false;
        if (cookie != null) {
            guestId = cookie.getValue();
        } else {
            guestId = UUID.randomUUID().toString();
            isNewGuest = true;
        }

        // X-Guest-Id 헤더빌드
        ServerHttpRequest newRequest = request.mutate()
                .header("X-Guest-Id", guestId)
                .build();

        //새 비회원이면 브라우저에도 쿠키를 생성해야함.
        if (isNewGuest){
            ResponseCookie responseCookie = ResponseCookie.from("GUEST_ID", guestId)
                    .maxAge(60 * 60 * 24 * 30) // 30일 유지
                    .path("/")
                    .httpOnly(true)
                    .secure(true)
                    .build();
            exchange.getResponse().addCookie(responseCookie);
        }

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        //AuthorizationHeaderFilter보다 우선순위
        return -1;
    }
}
