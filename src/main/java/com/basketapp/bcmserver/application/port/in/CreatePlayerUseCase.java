package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Player;

/**
 * Inbound port for creating a new Player.
 */
public interface CreatePlayerUseCase {

    Player createPlayer(CreatePlayerCommand command);
}

