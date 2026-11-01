package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for updating player match statistics")
public record UpdatePlayerStatsRequest(
        @NotNull(message = "game_id is required")
        @Positive(message = "game_id must be a positive integer")
        @JsonProperty("game_id")
        @Schema(description = "Target game identifier", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Long gameId,

        @NotNull(message = "player_id is required")
        @Positive(message = "player_id must be a positive integer")
        @JsonProperty("player_id")
        @Schema(description = "Target player identifier", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,

        @Min(value = 0, message = "minutes must be greater than or equal to 0")
        @JsonProperty("minutes")
        @Schema(description = "Minutes played", example = "30", defaultValue = "0")
        Integer minutes,

        @Min(value = 0, message = "seconds must be between 0 and 59")
        @Max(value = 59, message = "seconds must be between 0 and 59")
        @JsonProperty("seconds")
        @Schema(description = "Seconds played (0 to 59)", example = "15", defaultValue = "0")
        Integer seconds,

        @Min(value = 0, message = "t1a must be greater than or equal to 0")
        @JsonProperty("t1a")
        @Schema(description = "Free throws attempted", example = "6", defaultValue = "0")
        Integer t1a,

        @Min(value = 0, message = "t1i must be greater than or equal to 0")
        @JsonProperty("t1i")
        @Schema(description = "Free throws made", example = "5", defaultValue = "0")
        Integer t1i,

        @Min(value = 0, message = "t2a must be greater than or equal to 0")
        @JsonProperty("t2a")
        @Schema(description = "2-point shots attempted", example = "12", defaultValue = "0")
        Integer t2a,

        @Min(value = 0, message = "t2i must be greater than or equal to 0")
        @JsonProperty("t2i")
        @Schema(description = "2-point shots made", example = "8", defaultValue = "0")
        Integer t2i,

        @Min(value = 0, message = "t3a must be greater than or equal to 0")
        @JsonProperty("t3a")
        @Schema(description = "3-point shots attempted", example = "5", defaultValue = "0")
        Integer t3a,

        @Min(value = 0, message = "t3i must be greater than or equal to 0")
        @JsonProperty("t3i")
        @Schema(description = "3-point shots made", example = "3", defaultValue = "0")
        Integer t3i,

        @Min(value = 0, message = "oreb must be greater than or equal to 0")
        @JsonProperty("oreb")
        @Schema(description = "Offensive rebounds", example = "2", defaultValue = "0")
        Integer oreb,

        @Min(value = 0, message = "dreb must be greater than or equal to 0")
        @JsonProperty("dreb")
        @Schema(description = "Defensive rebounds", example = "7", defaultValue = "0")
        Integer dreb,

        @Min(value = 0, message = "asis must be greater than or equal to 0")
        @JsonProperty("asis")
        @Schema(description = "Assists", example = "9", defaultValue = "0")
        Integer asis,

        @Min(value = 0, message = "rec must be greater than or equal to 0")
        @JsonProperty("rec")
        @Schema(description = "Steals / recoveries", example = "4", defaultValue = "0")
        Integer rec,

        @Min(value = 0, message = "per must be greater than or equal to 0")
        @JsonProperty("per")
        @Schema(description = "Turnovers / ball losses", example = "1", defaultValue = "0")
        Integer per,

        @Min(value = 0, message = "falt must be greater than or equal to 0")
        @JsonProperty("falt")
        @Schema(description = "Personal fouls", example = "2", defaultValue = "0")
        Integer falt,

        @JsonProperty("valuation")
        @Schema(description = "Performance Index Rating (PIR) score (signed integer)", example = "29", defaultValue = "0")
        Integer valuation
) {
    @AssertTrue(message = "t1i cannot exceed t1a")
    public boolean isT1Valid() {
        if (t1a != null && t1i != null) {
            return t1i <= t1a;
        }
        return true;
    }

    @AssertTrue(message = "t2i cannot exceed t2a")
    public boolean isT2Valid() {
        if (t2a != null && t2i != null) {
            return t2i <= t2a;
        }
        return true;
    }

    @AssertTrue(message = "t3i cannot exceed t3a")
    public boolean isT3Valid() {
        if (t3a != null && t3i != null) {
            return t3i <= t3a;
        }
        return true;
    }
}

