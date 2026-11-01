package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.PlayerStats;

import java.util.List;

/**
 * Inbound port for retrieving player statistics (by ID, all, or filtered by game/player).
 */
public interface GetPlayerStatsUseCase {

    PlayerStats getPlayerStatsById(Long id);

    List<PlayerStats> getAllPlayerStats();

    List<PlayerStats> getPlayerStatsByFilters(Long gameId, Long playerId);
}

