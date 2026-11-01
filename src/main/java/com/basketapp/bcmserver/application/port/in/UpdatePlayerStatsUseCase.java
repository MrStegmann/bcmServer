package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.PlayerStats;

/**
 * Inbound port for updating an existing PlayerStats record.
 */
public interface UpdatePlayerStatsUseCase {

    PlayerStats updatePlayerStats(UpdatePlayerStatsCommand command);
}

