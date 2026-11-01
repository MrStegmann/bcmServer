package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user authentication.
 */
@Schema(description = "User login credentials payload")
public record LoginRequest(
        @Schema(description = "Registered username", example = "john_doe")
        @NotBlank(message = "Username cannot be blank")
        String username,

        @Schema(description = "Account plaintext password", example = "SuperSecret123!")
        @NotBlank(message = "Password cannot be blank")
        String password
) {}

