package com.food.smart_food_system.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;

import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String JWT_SECRET = "bXlfc3VwZXJfc2VjcmV0X2tleV9tdXN0X2JlX3ZlcnlfbG9uZ19hbmRfc2VjdXJlX2Zvcl9IUzUxMg==";
    // ⚠️ >= 32 ký tự (quan trọng)

    private final long JWT_EXPIRATION = 86400000; // 1 ngày

    // 🔐 Tạo key đúng chuẩn
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    // 🚀 Generate token
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 🔍 Lấy username từ token
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // ✅ Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}