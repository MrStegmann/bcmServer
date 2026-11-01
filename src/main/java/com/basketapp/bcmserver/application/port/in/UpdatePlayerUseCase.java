package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Player;

/**
 * Inbound port for updating an existing Player.
 */
public interface UpdatePlayerUseCase {

    Player updatePlayer(UpdatePlayerCommand command);
}

