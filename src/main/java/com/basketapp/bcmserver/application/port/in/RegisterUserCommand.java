package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound command encapsulating user registration input.
 */
public record RegisterUserCommand(
        String username,
        String password
) {}

