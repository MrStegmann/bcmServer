package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.out.GameRepositoryPort;
import com.basketapp.bcmserver.application.port.out.PlayerRepositoryPort;
import com.basketapp.bcmserver.application.port.out.PlayerStatsRepositoryPort;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerStatsNotFoundException;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service orchestrating PlayerStats use cases and business operations.
 */
@Service
@Transactional
public class PlayerStatsService implements CreatePlayerStatsUseCase, GetPlayerStatsUseCase, UpdatePlayerStatsUseCase, DeletePlayerStatsUseCase {

    private final PlayerStatsRepositoryPort playerStatsRepository;
    private final GameRepositoryPort gameRepository;
    private final PlayerRepositoryPort playerRepository;

    public PlayerStatsService(
            PlayerStatsRepositoryPort playerStatsRepository,
            GameRepositoryPort gameRepository,
            PlayerRepositoryPort playerRepository
    ) {
        this.playerStatsRepository = playerStatsRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public PlayerStats createPlayerStats(CreatePlayerStatsCommand command) {
        if (!gameRepository.existsById(command.gameId())) {
            throw new GameNotFoundException(command.gameId());
        }
        if (!playerRepository.existsById(command.playerId())) {
            throw new PlayerNotFoundException(command.playerId());
        }

        PlayerStats stats = PlayerStats.create(
                command.gameId(),
                command.playerId(),
                command.minutes(),
                command.seconds(),
                command.t1a(),
                command.t1i(),
                command.t2a(),
                command.t2i(),
                command.t3a(),
                command.t3i(),
                command.oreb(),
                command.dreb(),
                command.asis(),
                command.rec(),
                command.per(),
                command.falt(),
                command.valuation()
        );

        return playerStatsRepository.save(stats);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerStats getPlayerStatsById(Long id) {
        return playerStatsRepository.findById(id)
                .orElseThrow(() -> new PlayerStatsNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> getAllPlayerStats() {
        return playerStatsRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> getPlayerStatsByFilters(Long gameId, Long playerId) {
        if (gameId != null && playerId != null) {
            return playerStatsRepository.findByGameIdAndPlayerId(gameId, playerId);
        } else if (gameId != null) {
            return playerStatsRepository.findByGameId(gameId);
        } else if (playerId != null) {
            return playerStatsRepository.findByPlayerId(playerId);
        }
        return playerStatsRepository.findAll();
    }

    @Override
    public PlayerStats updatePlayerStats(UpdatePlayerStatsCommand command) {
        PlayerStats stats = playerStatsRepository.findById(command.id())
                .orElseThrow(() -> new PlayerStatsNotFoundException(command.id()));

        if (!gameRepository.existsById(command.gameId())) {
            throw new GameNotFoundException(command.gameId());
        }
        if (!playerRepository.existsById(command.playerId())) {
            throw new PlayerNotFoundException(command.playerId());
        }

        stats.update(
                command.gameId(),
                command.playerId(),
                command.minutes() != null ? command.minutes() : 0,
                command.seconds() != null ? command.seconds() : 0,
                command.t1a() != null ? command.t1a() : 0,
                command.t1i() != null ? command.t1i() : 0,
                command.t2a() != null ? command.t2a() : 0,
                command.t2i() != null ? command.t2i() : 0,
                command.t3a() != null ? command.t3a() : 0,
                command.t3i() != null ? command.t3i() : 0,
                command.oreb() != null ? command.oreb() : 0,
                command.dreb() != null ? command.dreb() : 0,
                command.asis() != null ? command.asis() : 0,
                command.rec() != null ? command.rec() : 0,
                command.per() != null ? command.per() : 0,
                command.falt() != null ? command.falt() : 0,
                command.valuation() != null ? command.valuation() : 0
        );

        return playerStatsRepository.save(stats);
    }

    @Override
    public void deletePlayerStats(Long id) {
        if (!playerStatsRepository.existsById(id)) {
            throw new PlayerStatsNotFoundException(id);
        }
        playerStatsRepository.deleteById(id);
    }
}
