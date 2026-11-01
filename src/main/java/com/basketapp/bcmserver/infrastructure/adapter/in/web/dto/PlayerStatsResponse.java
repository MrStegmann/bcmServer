package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of an individual player match statistics record")
public record PlayerStatsResponse(
        @Schema(description = "Unique numeric identifier of the statistics record", example = "1")
        @JsonProperty("id")
        Long id,

        @Schema(description = "Target game identifier", example = "10")
        @JsonProperty("game_id")
        Long gameId,

        @Schema(description = "Target player identifier", example = "4")
        @JsonProperty("player_id")
        Long playerId,

        @Schema(description = "Minutes played", example = "28")
        @JsonProperty("minutes")
        int minutes,

        @Schema(description = "Seconds played (0 to 59)", example = "45")
        @JsonProperty("seconds")
        int seconds,

        @Schema(description = "Free throws attempted", example = "6")
        @JsonProperty("t1a")
        int t1a,

        @Schema(description = "Free throws made", example = "5")
        @JsonProperty("t1i")
        int t1i,

        @Schema(description = "2-point shots attempted", example = "10")
        @JsonProperty("t2a")
        int t2a,

        @Schema(description = "2-point shots made", example = "7")
        @JsonProperty("t2i")
        int t2i,

        @Schema(description = "3-point shots attempted", example = "5")
        @JsonProperty("t3a")
        int t3a,

        @Schema(description = "3-point shots made", example = "2")
        @JsonProperty("t3i")
        int t3i,

        @Schema(description = "Offensive rebounds", example = "2")
        @JsonProperty("oreb")
        int oreb,

        @Schema(description = "Defensive rebounds", example = "6")
        @JsonProperty("dreb")
        int dreb,

        @Schema(description = "Assists", example = "8")
        @JsonProperty("asis")
        int asis,

        @Schema(description = "Steals / recoveries", example = "3")
        @JsonProperty("rec")
        int rec,

        @Schema(description = "Turnovers / ball losses", example = "2")
        @JsonProperty("per")
        int per,

        @Schema(description = "Personal fouls", example = "3")
        @JsonProperty("falt")
        int falt,

        @Schema(description = "Performance Index Rating (PIR) score", example = "24")
        @JsonProperty("valuation")
        int valuation
) {}

