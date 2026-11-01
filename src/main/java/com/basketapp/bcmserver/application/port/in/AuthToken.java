package com.basketapp.bcmserver.application.port.in;

/**
 * Value object representing an issued authentication token.
 */
public record AuthToken(
        String token,
        String tokenType,
        long expiresIn
) {
    public static AuthToken bearer(String token, long expiresIn) {
        return new AuthToken(token, "Bearer", expiresIn);
    }
}

