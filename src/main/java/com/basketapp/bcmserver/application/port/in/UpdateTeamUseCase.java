package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Team;

/**
 * Inbound port for updating an existing Team.
 */
public interface UpdateTeamUseCase {

    Team updateTeam(UpdateTeamCommand command);
}

