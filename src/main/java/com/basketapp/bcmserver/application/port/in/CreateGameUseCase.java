package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Game;

/**
 * Inbound port for creating a new Game.
 */
public interface CreateGameUseCase {

    Game createGame(CreateGameCommand command);
}

