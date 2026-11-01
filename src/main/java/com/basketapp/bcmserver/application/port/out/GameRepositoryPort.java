package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.domain.model.Game;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for game persistence operations.
 * Decouples domain and application logic from Spring Data / JPA.
 */
public interface GameRepositoryPort {

    Game save(Game game);

    Optional<Game> findById(Long id);

    List<Game> findAll();

    List<Game> findByTeamId(Long teamId);

    void deleteById(Long id);

    boolean existsById(Long id);
}

