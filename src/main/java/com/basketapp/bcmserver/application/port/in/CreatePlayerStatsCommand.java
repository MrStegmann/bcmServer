package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to create a new PlayerStats record.
 */
public record CreatePlayerStatsCommand(
        Long gameId,
        Long playerId,
        Integer minutes,
        Integer seconds,
        Integer t1a,
        Integer t1i,
        Integer t2a,
        Integer t2i,
        Integer t3a,
        Integer t3i,
        Integer oreb,
        Integer dreb,
        Integer asis,
        Integer rec,
        Integer per,
        Integer falt,
        Integer valuation
) {}

