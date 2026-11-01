package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound request payload for modifying an existing basketball team.
 */
@Schema(description = "Request payload for updating a team")
public record UpdateTeamRequest(
        @Schema(description = "Updated team name (cannot be blank)", example = "Senior Femenino Autonomico", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Team name is required and cannot be blank")
        @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
        String name,

        @Schema(description = "Updated competition category", example = "Senior", nullable = true)
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category
) {}

