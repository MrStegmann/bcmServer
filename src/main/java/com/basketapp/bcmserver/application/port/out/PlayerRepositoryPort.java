package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.domain.model.Player;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for player persistence operations.
 * Decouples domain and application logic from Spring Data / JPA.
 */
public interface PlayerRepositoryPort {

    Player save(Player player);

    Optional<Player> findById(Long id);

    List<Player> findAll();

    List<Player> findByTeamId(Long teamId);

    void deleteById(Long id);

    boolean existsById(Long id);
}

