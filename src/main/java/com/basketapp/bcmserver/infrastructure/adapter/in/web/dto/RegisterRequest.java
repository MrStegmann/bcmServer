package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for registering a new user account.
 */
@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Unique username (alphanumeric, underscore, dot, hyphen)", example = "john_doe")
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username can only contain alphanumeric characters, underscores, hyphens, and dots")
        String username,

        @Schema(description = "Plaintext password (minimum 8 characters)", example = "SuperSecret123!")
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {}

