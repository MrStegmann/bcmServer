package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to create a new Player.
 */
public record CreatePlayerCommand(
        Long teamId,
        String firstName,
        String lastName,
        String number,
        String jerseyName
) {}

