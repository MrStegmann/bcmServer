package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating user profile attributes.
 */
@Schema(description = "User profile update payload")
public record UpdateUserRequest(
        @Schema(description = "Optional updated username", example = "john_doe_new")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username can only contain alphanumeric characters, underscores, hyphens, and dots")
        String username,

        @Schema(description = "Optional updated plaintext password", example = "NewSuperSecret123!")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {}

