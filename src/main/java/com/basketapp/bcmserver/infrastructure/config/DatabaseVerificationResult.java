package com.basketapp.bcmserver.infrastructure.config;

import java.time.Instant;

public record DatabaseVerificationResult(
        VerificationStatus status,
        String databaseProductName,
        String databaseProductVersion,
        long validationLatencyMs,
        String errorMessage,
        Instant timestamp
) {
    public static DatabaseVerificationResult success(String productName, String productVersion, long latencyMs) {
        return new DatabaseVerificationResult(
                VerificationStatus.CONNECTED,
                productName,
                productVersion,
                latencyMs,
                null,
                Instant.now()
        );
    }

    public static DatabaseVerificationResult failure(VerificationStatus status, String errorMessage, long latencyMs) {
        return new DatabaseVerificationResult(
                status,
                null,
                null,
                latencyMs,
                errorMessage,
                Instant.now()
        );
    }
}

