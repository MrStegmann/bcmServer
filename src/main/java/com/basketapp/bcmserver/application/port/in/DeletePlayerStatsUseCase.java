package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for deleting a player statistics record.
 */
public interface DeletePlayerStatsUseCase {

    void deletePlayerStats(Long id);
}

