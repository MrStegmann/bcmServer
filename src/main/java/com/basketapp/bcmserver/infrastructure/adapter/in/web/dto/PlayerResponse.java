package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Public response representation of a player record.
 */
@Schema(description = "Representation of a player record")
public record PlayerResponse(
        @Schema(description = "Unique numeric identifier of the player", example = "1")
        Long id,

        @Schema(description = "Unique numeric identifier of the associated team", example = "1")
        Long teamId,

        @Schema(description = "Player's first name", example = "Michael")
        String firstName,

        @Schema(description = "Player's last name", example = "Jordan")
        String lastName,

        @Schema(description = "Player's jersey/dorsal number", example = "23")
        String number,

        @Schema(description = "Player's jersey display name", example = "JORDAN", nullable = true)
        String jerseyName,

        @Schema(description = "UTC timestamp when the record was created", example = "2026-10-31T12:00:00Z")
        Instant createdAt
) {}

