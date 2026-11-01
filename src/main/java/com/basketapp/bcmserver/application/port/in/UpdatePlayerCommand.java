package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to update an existing Player.
 */
public record UpdatePlayerCommand(
        Long id,
        Long teamId,
        String firstName,
        String lastName,
        String number,
        String jerseyName
) {}

