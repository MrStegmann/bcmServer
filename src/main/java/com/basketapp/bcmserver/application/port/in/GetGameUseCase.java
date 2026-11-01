package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Game;

import java.util.List;

/**
 * Inbound port for retrieving games (by ID, all, or filtered by team).
 */
public interface GetGameUseCase {

    Game findById(Long id);

    List<Game> findAll();

    List<Game> findByTeamId(Long teamId);
}

