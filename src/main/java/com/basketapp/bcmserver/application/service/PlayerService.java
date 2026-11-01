package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.CreatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerUseCase;
import com.basketapp.bcmserver.application.port.out.PlayerRepositoryPort;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service orchestrating Player use cases and business operations.
 */
@Service
@Transactional
public class PlayerService implements CreatePlayerUseCase, GetPlayerUseCase, UpdatePlayerUseCase, DeletePlayerUseCase {

    private final PlayerRepositoryPort playerRepository;
    private final TeamRepositoryPort teamRepository;

    public PlayerService(PlayerRepositoryPort playerRepository, TeamRepositoryPort teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public Player createPlayer(CreatePlayerCommand command) {
        if (!teamRepository.existsById(command.teamId())) {
            throw new TeamNotFoundException(command.teamId());
        }
        Player player = Player.create(
                command.teamId(),
                command.firstName(),
                command.lastName(),
                command.number(),
                command.jerseyName()
        );
        return playerRepository.save(player);
    }

    @Override
    @Transactional(readOnly = true)
    public Player findById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findByTeamId(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    @Override
    public Player updatePlayer(UpdatePlayerCommand command) {
        Player player = playerRepository.findById(command.id())
                .orElseThrow(() -> new PlayerNotFoundException(command.id()));
        if (!teamRepository.existsById(command.teamId())) {
            throw new TeamNotFoundException(command.teamId());
        }
        player.update(
                command.teamId(),
                command.firstName(),
                command.lastName(),
                command.number(),
                command.jerseyName()
        );
        return playerRepository.save(player);
    }

    @Override
    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new PlayerNotFoundException(id);
        }
        playerRepository.deleteById(id);
    }
}

