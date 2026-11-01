package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound command encapsulating profile update parameters with requester ID for ownership verification.
 */
public record UpdateUserCommand(
        Long targetUserId,
        Long requesterUserId,
        String username,
        String password
) {}

