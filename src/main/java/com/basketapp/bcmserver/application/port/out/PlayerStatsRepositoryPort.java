package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.domain.model.PlayerStats;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for player statistics persistence operations.
 * Decouples domain and application logic from Spring Data / JPA.
 */
public interface PlayerStatsRepositoryPort {

    PlayerStats save(PlayerStats playerStats);

    Optional<PlayerStats> findById(Long id);

    List<PlayerStats> findAll();

    List<PlayerStats> findByGameId(Long gameId);

    List<PlayerStats> findByPlayerId(Long playerId);

    List<PlayerStats> findByGameIdAndPlayerId(Long gameId, Long playerId);

    void deleteById(Long id);

    boolean existsById(Long id);
}

