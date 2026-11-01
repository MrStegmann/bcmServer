package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.PlayerStats;

/**
 * Inbound port for creating a new PlayerStats record.
 */
public interface CreatePlayerStatsUseCase {

    PlayerStats createPlayerStats(CreatePlayerStatsCommand command);
}

