package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateGameRequest(
        @NotNull(message = "team_id is required")
        @Positive(message = "team_id must be a positive number")
        @JsonProperty("team_id")
        Long teamId,

        @NotBlank(message = "opponent cannot be blank")
        @Size(max = 255, message = "opponent cannot exceed 255 characters")
        @JsonProperty("opponent")
        String opponent,

        @NotBlank(message = "round cannot be blank")
        @JsonProperty("round")
        String round,

        @NotBlank(message = "date cannot be blank")
        @JsonProperty("date")
        String date,

        @NotBlank(message = "status cannot be blank")
        @Pattern(regexp = "^(pending|live|finished)$", message = "status must be one of: pending, live, finished")
        @JsonProperty("status")
        String status,

        // Scores (Required on update, must be >= 0)
        @NotNull(message = "result_c1 is required") @Min(value = 0, message = "result_c1 must be >= 0") @JsonProperty("result_c1") Integer resultC1,
        @NotNull(message = "result_c2 is required") @Min(value = 0, message = "result_c2 must be >= 0") @JsonProperty("result_c2") Integer resultC2,
        @NotNull(message = "result_c3 is required") @Min(value = 0, message = "result_c3 must be >= 0") @JsonProperty("result_c3") Integer resultC3,
        @NotNull(message = "result_c4 is required") @Min(value = 0, message = "result_c4 must be >= 0") @JsonProperty("result_c4") Integer resultC4,
        @NotNull(message = "result_extra is required") @Min(value = 0, message = "result_extra must be >= 0") @JsonProperty("result_extra") Integer resultExtra,
        @NotNull(message = "result_c1_opponent is required") @Min(value = 0, message = "result_c1_opponent must be >= 0") @JsonProperty("result_c1_opponent") Integer resultC1Opponent,
        @NotNull(message = "result_c2_opponent is required") @Min(value = 0, message = "result_c2_opponent must be >= 0") @JsonProperty("result_c2_opponent") Integer resultC2Opponent,
        @NotNull(message = "result_c3_opponent is required") @Min(value = 0, message = "result_c3_opponent must be >= 0") @JsonProperty("result_c3_opponent") Integer resultC3Opponent,
        @NotNull(message = "result_c4_opponent is required") @Min(value = 0, message = "result_c4_opponent must be >= 0") @JsonProperty("result_c4_opponent") Integer resultC4Opponent,
        @NotNull(message = "result_extra_opponent is required") @Min(value = 0, message = "result_extra_opponent must be >= 0") @JsonProperty("result_extra_opponent") Integer resultExtraOpponent,

        // Fouls (Required on update, must be >= 0)
        @NotNull(message = "fouls_c1 is required") @Min(value = 0, message = "fouls_c1 must be >= 0") @JsonProperty("fouls_c1") Integer foulsC1,
        @NotNull(message = "fouls_c2 is required") @Min(value = 0, message = "fouls_c2 must be >= 0") @JsonProperty("fouls_c2") Integer foulsC2,
        @NotNull(message = "fouls_c3 is required") @Min(value = 0, message = "fouls_c3 must be >= 0") @JsonProperty("fouls_c3") Integer foulsC3,
        @NotNull(message = "fouls_c4 is required") @Min(value = 0, message = "fouls_c4 must be >= 0") @JsonProperty("fouls_c4") Integer foulsC4,
        @NotNull(message = "fouls_extra is required") @Min(value = 0, message = "fouls_extra must be >= 0") @JsonProperty("fouls_extra") Integer foulsExtra,
        @NotNull(message = "fouls_c1_opponent is required") @Min(value = 0, message = "fouls_c1_opponent must be >= 0") @JsonProperty("fouls_c1_opponent") Integer foulsC1Opponent,
        @NotNull(message = "fouls_c2_opponent is required") @Min(value = 0, message = "fouls_c2_opponent must be >= 0") @JsonProperty("fouls_c2_opponent") Integer foulsC2Opponent,
        @NotNull(message = "fouls_c3_opponent is required") @Min(value = 0, message = "fouls_c3_opponent must be >= 0") @JsonProperty("fouls_c3_opponent") Integer foulsC3Opponent,
        @NotNull(message = "fouls_c4_opponent is required") @Min(value = 0, message = "fouls_c4_opponent must be >= 0") @JsonProperty("fouls_c4_opponent") Integer foulsC4Opponent,
        @NotNull(message = "fouls_extra_opponent is required") @Min(value = 0, message = "fouls_extra_opponent must be >= 0") @JsonProperty("fouls_extra_opponent") Integer foulsExtraOpponent
) {}

