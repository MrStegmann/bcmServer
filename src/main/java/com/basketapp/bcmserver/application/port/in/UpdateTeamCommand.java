package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data to update an existing Team.
 */
public record UpdateTeamCommand(
        Long id,
        String name,
        String category
) {}

