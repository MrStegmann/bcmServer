package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Inbound request payload for updating an existing player's details.
 */
@Schema(description = "Payload for updating an existing player's details")
public record UpdatePlayerRequest(
        @NotNull(message = "Team ID is required")
        @Positive(message = "Team ID must be a positive number")
        @Schema(description = "ID of the associated team", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long teamId,

        @NotBlank(message = "First name is required and cannot be blank")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        @Schema(description = "Player's first name", example = "Michael", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @NotBlank(message = "Last name is required and cannot be blank")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        @Schema(description = "Player's last name", example = "Jordan", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @NotBlank(message = "Jersey number is required and cannot be blank")
        @Size(min = 1, max = 10, message = "Jersey number must be between 1 and 10 characters")
        @Schema(description = "Jersey / dorsal number", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
        String number,

        @Size(max = 50, message = "Jersey name must not exceed 50 characters")
        @Schema(description = "Optional name printed on the jersey", example = "JORDAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String jerseyName
) {}

