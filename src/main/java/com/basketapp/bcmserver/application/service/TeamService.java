package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.CreateTeamCommand;
import com.basketapp.bcmserver.application.port.in.CreateTeamUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteTeamUseCase;
import com.basketapp.bcmserver.application.port.in.GetTeamUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateTeamCommand;
import com.basketapp.bcmserver.application.port.in.UpdateTeamUseCase;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service orchestrating Team use cases and business operations.
 */
@Service
@Transactional
public class TeamService implements CreateTeamUseCase, GetTeamUseCase, UpdateTeamUseCase, DeleteTeamUseCase {

    private final TeamRepositoryPort teamRepository;

    public TeamService(TeamRepositoryPort teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Override
    public Team createTeam(CreateTeamCommand command) {
        Team team = Team.create(command.name(), command.category());
        return teamRepository.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAll() {
        return teamRepository.findAll();
    }

    @Override
    public Team updateTeam(UpdateTeamCommand command) {
        Team team = teamRepository.findById(command.id())
                .orElseThrow(() -> new TeamNotFoundException(command.id()));
        team.update(command.name(), command.category());
        return teamRepository.save(team);
    }

    @Override
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new TeamNotFoundException(id);
        }
        teamRepository.deleteById(id);
    }
}
