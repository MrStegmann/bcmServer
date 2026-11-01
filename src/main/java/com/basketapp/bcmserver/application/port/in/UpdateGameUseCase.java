package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Game;

/**
 * Inbound port for updating an existing Game.
 */
public interface UpdateGameUseCase {

    Game updateGame(UpdateGameCommand command);
}

