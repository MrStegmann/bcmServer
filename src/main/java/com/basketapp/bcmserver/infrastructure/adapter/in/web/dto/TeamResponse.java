package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Public response payload for Team details.
 */
@Schema(description = "Team public representation")
public record TeamResponse(
        @Schema(description = "Unique numeric identifier of the team", example = "1")
        Long id,

        @Schema(description = "Mandatory, non-empty team name", example = "Cadete Femenino A")
        String name,

        @Schema(description = "Optional competition category or division", example = "U16 Federado", nullable = true)
        String category,

        @Schema(description = "Timestamp when the team was created in UTC", example = "2026-10-30T10:00:00Z")
        Instant createdAt
) {}

