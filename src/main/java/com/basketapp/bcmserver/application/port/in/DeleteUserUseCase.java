package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for permanently deleting an account with self-service authorization.
 */
public interface DeleteUserUseCase {

    void deleteUser(Long targetUserId, Long requesterUserId);
}

