package com.bae4.drivedrop.utils;

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

public class JWTUtil {
    private final static SecretKey key =
            Keys.hmacShaKeyFor("dev-secret-key-please-change-this-in-prod-12345678901234567890"
                    .getBytes());
    public static String generateToken(String email, String googleSub) {
        return Jwts.builder()
                .subject(googleSub)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000 * 7))
                .signWith(key)
                .compact();
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Token Validation Failed: " + e.getMessage());
            return null;
        }
    }
}
