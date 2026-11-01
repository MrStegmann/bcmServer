package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for deleting an existing Player by identifier.
 */
public interface DeletePlayerUseCase {

    void deletePlayer(Long id);
}

