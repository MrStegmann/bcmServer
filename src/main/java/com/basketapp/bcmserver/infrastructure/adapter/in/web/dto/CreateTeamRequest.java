package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound request payload for creating a basketball team.
 */
@Schema(description = "Request payload for creating a team")
public record CreateTeamRequest(
        @Schema(description = "Mandatory, non-empty team name", example = "Cadete Femenino A", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Team name is required and cannot be blank")
        @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
        String name,

        @Schema(description = "Optional competition category or division", example = "U16 Federado", nullable = true)
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category
) {}

