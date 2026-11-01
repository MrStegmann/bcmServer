package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload containing issued JWT bearer token and user profile.
 */
@Schema(description = "Authentication response with bearer token")
public record AuthResponse(
        @Schema(description = "Signed JWT bearer token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Token authentication type", example = "Bearer")
        String tokenType,

        @Schema(description = "Token validity lifetime in seconds", example = "86400")
        long expiresIn,

        @Schema(description = "Authenticated user profile details")
        UserProfileResponse user
) {}

