package com.nhnacademy.book2onandongateway.filter;

import com.nhnacademy.book2onandongateway.filter.AuthorizationHeaderFilter.Config;
import com.nhnacademy.book2onandongateway.util.JwtTokenProvider;
import com.nhnacademy.book2onandongateway.util.RedisUtil;
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
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;

    public AuthorizationHeaderFilter(JwtTokenProvider jwtTokenProvider, RedisUtil redisUtil) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisUtil = redisUtil;
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

            // Authorization 헤더 추출
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 헤더 없거나 빈 값이면 토큰 없는 요청 -> 필터 스킵
            if (authHeader == null || authHeader.isBlank()) {
                return chain.filter(exchange);
            }

            // Bearer 형식 확인
            if (!authHeader.startsWith("Bearer ")) {
                return onError(exchange, "인증 헤더 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            //로그아웃 블랙리스트 확인
            if (redisUtil.hasKeyBlackList(token)) {
                return onError(exchange, "로그아웃된 토큰입니다. 접근이 거부됩니다.", HttpStatus.UNAUTHORIZED);
            }

            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return onError(exchange, "유효하지 않거나 만료된 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtTokenProvider.getUserId(token);
            String userRole = jwtTokenProvider.getRole(token);

            // 권한 검사
            if (config.getRole() != null) {
                boolean isMatched = config.getRole().equals(userRole);
                boolean isSuperAdmin = "ROLE_SUPER_ADMIN".equals(userRole);

                if (!isMatched && !isSuperAdmin) {
                    return onError(exchange, "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
                }
            }

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