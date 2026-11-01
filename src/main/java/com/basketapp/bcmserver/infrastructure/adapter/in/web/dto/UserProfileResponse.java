package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Public response payload for user profile details.
 * Strictly excludes any password or hash fields.
 */
@Schema(description = "User profile public details")
public record UserProfileResponse(
        @Schema(description = "Unique user identifier", example = "1")
        Long id,

        @Schema(description = "User's unique registered username", example = "john_doe")
        String username,

        @Schema(description = "Account status", example = "ACTIVE")
        String status,

        @Schema(description = "Account registration timestamp in UTC", example = "2026-10-29T14:30:00Z")
        Instant createdAt
) {
    public UserProfileResponse(Long id, String username, Instant createdAt) {
        this(id, username, "ACTIVE", createdAt);
    }
}
