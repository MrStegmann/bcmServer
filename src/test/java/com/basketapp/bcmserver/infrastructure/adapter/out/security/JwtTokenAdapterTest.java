package com.basketapp.bcmserver.infrastructure.adapter.out.security;

import com.basketapp.bcmserver.application.port.in.AuthToken;
import com.basketapp.bcmserver.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenAdapterTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtTokenAdapter jwtTokenAdapter;

    @BeforeEach
    void setUp() {
        jwtTokenAdapter = new JwtTokenAdapter(SECRET, 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate valid signed JWT token containing user claims")
    void shouldGenerateValidToken() {
        User user = User.reconstitute(42L, "alex_coach", "$2a$12$hashed", Instant.now());

        AuthToken authToken = jwtTokenAdapter.generateToken(user);

        assertNotNull(authToken);
        assertNotNull(authToken.token());
        assertEquals("Bearer", authToken.tokenType());
        assertEquals(3600L, authToken.expiresIn());

        assertTrue(jwtTokenAdapter.validateToken(authToken.token()));
        assertEquals("alex_coach", jwtTokenAdapter.getUsernameFromToken(authToken.token()));
        assertEquals(42L, jwtTokenAdapter.getUserIdFromToken(authToken.token()));
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        JwtTokenAdapter expiredAdapter = new JwtTokenAdapter(SECRET, -1000L); // expired 1s ago
        User user = User.reconstitute(1L, "expired_user", "$2a$12$hashed", Instant.now());

        AuthToken token = expiredAdapter.generateToken(user);

        assertFalse(jwtTokenAdapter.validateToken(token.token()));
    }

    @Test
    @DisplayName("Should reject tampered or malformed token")
    void shouldRejectTamperedToken() {
        User user = User.reconstitute(1L, "valid_user", "$2a$12$hashed", Instant.now());
        AuthToken authToken = jwtTokenAdapter.generateToken(user);

        String tampered = authToken.token() + "corrupted";
        assertFalse(jwtTokenAdapter.validateToken(tampered));
        assertFalse(jwtTokenAdapter.validateToken("not.a.valid.jwt"));
        assertFalse(jwtTokenAdapter.validateToken(null));
        assertFalse(jwtTokenAdapter.validateToken("   "));
    }

    @Test
    @DisplayName("Should return null for non-existent or invalid userId claims")
    void shouldHandleMissingClaimsGracefully() {
        assertNull(jwtTokenAdapter.getUserIdFromToken(""));
    }
}

