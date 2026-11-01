package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record GameResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("team_id") Long teamId,
        @JsonProperty("opponent") String opponent,
        @JsonProperty("round") String round,
        @JsonProperty("date") String date,
        @JsonProperty("status") String status,
        @JsonProperty("created_at") Instant createdAt,

        @JsonProperty("result_c1") int resultC1,
        @JsonProperty("result_c2") int resultC2,
        @JsonProperty("result_c3") int resultC3,
        @JsonProperty("result_c4") int resultC4,
        @JsonProperty("result_extra") int resultExtra,
        @JsonProperty("result_c1_opponent") int resultC1Opponent,
        @JsonProperty("result_c2_opponent") int resultC2Opponent,
        @JsonProperty("result_c3_opponent") int resultC3Opponent,
        @JsonProperty("result_c4_opponent") int resultC4Opponent,
        @JsonProperty("result_extra_opponent") int resultExtraOpponent,

        @JsonProperty("fouls_c1") int foulsC1,
        @JsonProperty("fouls_c2") int foulsC2,
        @JsonProperty("fouls_c3") int foulsC3,
        @JsonProperty("fouls_c4") int foulsC4,
        @JsonProperty("fouls_extra") int foulsExtra,
        @JsonProperty("fouls_c1_opponent") int foulsC1Opponent,
        @JsonProperty("fouls_c2_opponent") int foulsC2Opponent,
        @JsonProperty("fouls_c3_opponent") int foulsC3Opponent,
        @JsonProperty("fouls_c4_opponent") int foulsC4Opponent,
        @JsonProperty("fouls_extra_opponent") int foulsExtraOpponent
) {}

