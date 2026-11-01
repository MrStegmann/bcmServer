package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for deleting an existing Game by identifier.
 */
public interface DeleteGameUseCase {

    void deleteGame(Long id);
}

