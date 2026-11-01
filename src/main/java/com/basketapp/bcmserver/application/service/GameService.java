package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.CreateGameCommand;
import com.basketapp.bcmserver.application.port.in.CreateGameUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteGameUseCase;
import com.basketapp.bcmserver.application.port.in.GetGameUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateGameCommand;
import com.basketapp.bcmserver.application.port.in.UpdateGameUseCase;
import com.basketapp.bcmserver.application.port.out.GameRepositoryPort;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service orchestrating Game use cases and business operations.
 */
@Service
@Transactional
public class GameService implements CreateGameUseCase, GetGameUseCase, UpdateGameUseCase, DeleteGameUseCase {

    private final GameRepositoryPort gameRepository;
    private final TeamRepositoryPort teamRepository;

    public GameService(GameRepositoryPort gameRepository, TeamRepositoryPort teamRepository) {
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public Game createGame(CreateGameCommand command) {
        if (!teamRepository.existsById(command.teamId())) {
            throw new TeamNotFoundException(command.teamId());
        }

        GameStatus status = command.status() != null && !command.status().isBlank()
                ? GameStatus.fromValue(command.status())
                : GameStatus.PENDING;

        Game game = Game.create(
                command.teamId(),
                command.opponent(),
                command.round(),
                command.date(),
                status,
                command.resultC1(),
                command.resultC2(),
                command.resultC3(),
                command.resultC4(),
                command.resultExtra(),
                command.resultC1Opponent(),
                command.resultC2Opponent(),
                command.resultC3Opponent(),
                command.resultC4Opponent(),
                command.resultExtraOpponent(),
                command.foulsC1(),
                command.foulsC2(),
                command.foulsC3(),
                command.foulsC4(),
                command.foulsExtra(),
                command.foulsC1Opponent(),
                command.foulsC2Opponent(),
                command.foulsC3Opponent(),
                command.foulsC4Opponent(),
                command.foulsExtraOpponent()
        );

        return gameRepository.save(game);
    }

    @Override
    @Transactional(readOnly = true)
    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findAll() {
        return gameRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findByTeamId(Long teamId) {
        return gameRepository.findByTeamId(teamId);
    }

    @Override
    public Game updateGame(UpdateGameCommand command) {
        Game game = gameRepository.findById(command.id())
                .orElseThrow(() -> new GameNotFoundException(command.id()));

        if (!teamRepository.existsById(command.teamId())) {
            throw new TeamNotFoundException(command.teamId());
        }

        GameStatus status = GameStatus.fromValue(command.status());

        game.update(
                command.teamId(),
                command.opponent(),
                command.round(),
                command.date(),
                status,
                command.resultC1(),
                command.resultC2(),
                command.resultC3(),
                command.resultC4(),
                command.resultExtra(),
                command.resultC1Opponent(),
                command.resultC2Opponent(),
                command.resultC3Opponent(),
                command.resultC4Opponent(),
                command.resultExtraOpponent(),
                command.foulsC1(),
                command.foulsC2(),
                command.foulsC3(),
                command.foulsC4(),
                command.foulsExtra(),
                command.foulsC1Opponent(),
                command.foulsC2Opponent(),
                command.foulsC3Opponent(),
                command.foulsC4Opponent(),
                command.foulsExtraOpponent()
        );

        return gameRepository.save(game);
    }

    @Override
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new GameNotFoundException(id);
        }
        gameRepository.deleteById(id);
    }
}
