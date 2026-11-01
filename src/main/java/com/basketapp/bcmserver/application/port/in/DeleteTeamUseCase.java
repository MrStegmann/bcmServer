package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for deleting an existing Team.
 */
public interface DeleteTeamUseCase {

    void deleteTeam(Long id);
}

