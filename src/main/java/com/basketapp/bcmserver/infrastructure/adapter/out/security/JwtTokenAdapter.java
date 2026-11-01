package com.basketapp.bcmserver.infrastructure.adapter.out.security;

import com.basketapp.bcmserver.application.port.in.AuthToken;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Adapter implementing TokenProviderPort using JJWT 0.12.6.
 * Signs and validates stateless HMAC-SHA tokens without server-side session state.
 */
@Component
public class JwtTokenAdapter implements TokenProviderPort {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenAdapter(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.signingKey = resolveSigningKey(secret);
        this.expirationMs = expirationMs;
    }

    private SecretKey resolveSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public AuthToken generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();

        long expiresInSeconds = expirationMs / 1000;
        return AuthToken.bearer(token, expiresInSeconds);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    @Override
    public Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Claims claims = getClaims(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        } else if (userId != null) {
            return Long.parseLong(userId.toString());
        }
        return null;
    }

    private Claims getClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
