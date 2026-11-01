package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.Team;

import java.util.List;

/**
 * Inbound port for retrieving one or all basketball teams.
 */
public interface GetTeamUseCase {

    Team findById(Long id);

    List<Team> findAll();
}

