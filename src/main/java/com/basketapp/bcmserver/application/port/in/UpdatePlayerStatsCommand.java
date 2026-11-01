package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to update an existing PlayerStats record.
 */
public record UpdatePlayerStatsCommand(
        Long id,
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

