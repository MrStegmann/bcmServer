package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Team;

/**
 * Inbound port for creating a new Team.
 */
public interface CreateTeamUseCase {

    Team createTeam(CreateTeamCommand command);
}

