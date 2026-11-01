package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(
        @NotNull(message = "team_id is required")
        @Positive(message = "team_id must be a positive number")
        @JsonProperty("team_id")
        Long teamId,

        @NotBlank(message = "opponent cannot be blank")
        @Size(max = 255, message = "opponent cannot exceed 255 characters")
        @JsonProperty("opponent")
        String opponent,

        @JsonProperty("round")
        String round,

        @NotBlank(message = "date cannot be blank")
        @JsonProperty("date")
        String date,

        @Pattern(regexp = "^(pending|live|finished)?$", message = "status must be one of: pending, live, finished")
        @JsonProperty("status")
        String status,

        // Scores (Optional on create, default 0, must be >= 0)
        @Min(value = 0, message = "result_c1 must be >= 0") @JsonProperty("result_c1") Integer resultC1,
        @Min(value = 0, message = "result_c2 must be >= 0") @JsonProperty("result_c2") Integer resultC2,
        @Min(value = 0, message = "result_c3 must be >= 0") @JsonProperty("result_c3") Integer resultC3,
        @Min(value = 0, message = "result_c4 must be >= 0") @JsonProperty("result_c4") Integer resultC4,
        @Min(value = 0, message = "result_extra must be >= 0") @JsonProperty("result_extra") Integer resultExtra,
        @Min(value = 0, message = "result_c1_opponent must be >= 0") @JsonProperty("result_c1_opponent") Integer resultC1Opponent,
        @Min(value = 0, message = "result_c2_opponent must be >= 0") @JsonProperty("result_c2_opponent") Integer resultC2Opponent,
        @Min(value = 0, message = "result_c3_opponent must be >= 0") @JsonProperty("result_c3_opponent") Integer resultC3Opponent,
        @Min(value = 0, message = "result_c4_opponent must be >= 0") @JsonProperty("result_c4_opponent") Integer resultC4Opponent,
        @Min(value = 0, message = "result_extra_opponent must be >= 0") @JsonProperty("result_extra_opponent") Integer resultExtraOpponent,

        // Fouls (Optional on create, default 0, must be >= 0)
        @Min(value = 0, message = "fouls_c1 must be >= 0") @JsonProperty("fouls_c1") Integer foulsC1,
        @Min(value = 0, message = "fouls_c2 must be >= 0") @JsonProperty("fouls_c2") Integer foulsC2,
        @Min(value = 0, message = "fouls_c3 must be >= 0") @JsonProperty("fouls_c3") Integer foulsC3,
        @Min(value = 0, message = "fouls_c4 must be >= 0") @JsonProperty("fouls_c4") Integer foulsC4,
        @Min(value = 0, message = "fouls_extra must be >= 0") @JsonProperty("fouls_extra") Integer foulsExtra,
        @Min(value = 0, message = "fouls_c1_opponent must be >= 0") @JsonProperty("fouls_c1_opponent") Integer foulsC1Opponent,
        @Min(value = 0, message = "fouls_c2_opponent must be >= 0") @JsonProperty("fouls_c2_opponent") Integer foulsC2Opponent,
        @Min(value = 0, message = "fouls_c3_opponent must be >= 0") @JsonProperty("fouls_c3_opponent") Integer foulsC3Opponent,
        @Min(value = 0, message = "fouls_c4_opponent must be >= 0") @JsonProperty("fouls_c4_opponent") Integer foulsC4Opponent,
        @Min(value = 0, message = "fouls_extra_opponent must be >= 0") @JsonProperty("fouls_extra_opponent") Integer foulsExtraOpponent
) {}

