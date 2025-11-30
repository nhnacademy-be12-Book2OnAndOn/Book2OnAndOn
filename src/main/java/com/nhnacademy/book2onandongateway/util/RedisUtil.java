package com.nhnacademy.book2onandongateway.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

     //토큰이 블랙리스트에 있는지 확인
    public boolean hasKeyBlackList(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}