package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Player;

import java.util.List;

/**
 * Inbound port for retrieving players (by ID, all, or filtered by team).
 */
public interface GetPlayerUseCase {

    Player findById(Long id);

    List<Player> findAll();

    List<Player> findByTeamId(Long teamId);
}

