package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound command encapsulating user credentials for authentication.
 */
public record LoginUserCommand(
        String username,
        String password
) {}

