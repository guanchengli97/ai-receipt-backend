package com.example.aireceiptbackend.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtil {
    private static final String DEFAULT_SECRET = "replace_this_with_a_secure_secret";
    private static final long DEFAULT_EXPIRATION_MS = 24 * 60 * 60 * 1000L;
    private static final String SECRET = resolveSecret();
    private static final long EXPIRATION_MS = resolveExpirationMs();

    public static String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    public static String getSubjectFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveSecret() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.trim().isEmpty()) {
            return envSecret.trim();
        }

        String propSecret = System.getProperty("jwt.secret");
        if (propSecret != null && !propSecret.trim().isEmpty()) {
            return propSecret.trim();
        }

        return DEFAULT_SECRET;
    }

    private static long resolveExpirationMs() {
        String raw = System.getenv("JWT_EXPIRATION");
        if (raw == null || raw.trim().isEmpty()) {
            raw = System.getProperty("jwt.expiration");
        }
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_EXPIRATION_MS;
        }

        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_EXPIRATION_MS;
        }
    }
}
