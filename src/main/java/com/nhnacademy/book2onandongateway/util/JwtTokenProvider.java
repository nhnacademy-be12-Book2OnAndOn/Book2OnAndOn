package com.nhnacademy.book2onandongateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final Key secretKey;

    public JwtTokenProvider(@Value("${jwt.secret-key}") String secret){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserId(String token) {
        return parseClaims(token).get("userId").toString();
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}
