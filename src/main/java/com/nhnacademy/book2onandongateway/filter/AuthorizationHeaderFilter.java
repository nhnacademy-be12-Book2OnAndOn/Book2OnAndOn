package com.nhnacademy.book2onandongateway.filter;

import com.nhnacademy.book2onandongateway.util.JwtTokenProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthorizationHeaderFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Data
    public static class Config {
        private String role; //관리자 ROLE
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (isWhitelisted(path)) {
                return chain.filter(exchange);
            }

            // 헤더 존재 여부 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "인증 헤더가 없습니다.", HttpStatus.UNAUTHORIZED);
            }

            // Bearer 형식 확인 (Bearer: "토큰을 지니고 있는 인증된 사용자"라는 의미의 토큰)
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return onError(exchange, "인증 헤더 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
            }

            // 토큰 추출
            String token = authorizationHeader.substring(7);

            //토큰 유효성 검증 (위변조, 만료 등)
            if (!jwtTokenProvider.validateToken(token)) {
                return onError(exchange, "유효하지 않거나 만료된 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }
            String userId = jwtTokenProvider.getUserId(token);
            String userRole = jwtTokenProvider.getRole(token);


            // 권한검사
            if (config.getRole() != null) {

                boolean isMatched = config.getRole().equals(userRole);

                boolean isSuperAdmin = "ROLE_SUPER_ADMIN".equals(userRole);

                if (!isMatched && !isSuperAdmin) {
                    log.warn("권한 부족: User({}) Role({}) -> Required({})", userId, userRole, config.getRole());
                    return onError(exchange, "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
                }
            }

            // 헤더를 추가해서 전달
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", userRole)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }


    private boolean isWhitelisted(String path) {
        return path.startsWith("/auth") ||
                path.startsWith("/api/auth") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs");
    }


    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error("JWT Filter Error: {}", err);
        return response.setComplete();
    }
}