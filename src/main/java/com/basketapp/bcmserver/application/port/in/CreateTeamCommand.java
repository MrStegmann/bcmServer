package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to create a new Team.
 */
public record CreateTeamCommand(
        String name,
        String category
) {}

