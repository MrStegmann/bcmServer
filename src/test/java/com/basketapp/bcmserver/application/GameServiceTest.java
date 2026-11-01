package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.CreateGameCommand;
import com.basketapp.bcmserver.application.port.in.UpdateGameCommand;
import com.basketapp.bcmserver.application.port.out.GameRepositoryPort;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.application.service.GameService;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepositoryPort gameRepository;

    @Mock
    private TeamRepositoryPort teamRepository;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, teamRepository);
    }

    @Test
    @DisplayName("Should successfully create a new game when team exists")
    void shouldCreateGameSuccessfully() {
        CreateGameCommand command = new CreateGameCommand(
                1L, "Lakers", "1", "2026-11-01", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );

        when(teamRepository.existsById(1L)).thenReturn(true);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game toSave = invocation.getArgument(0);
            return Game.reconstitute(
                    10L, toSave.getTeamId(), toSave.getOpponent(), toSave.getRound(), toSave.getDate(),
                    toSave.getStatus(), Instant.now(),
                    toSave.getResultC1(), toSave.getResultC2(), toSave.getResultC3(), toSave.getResultC4(), toSave.getResultExtra(),
                    toSave.getResultC1Opponent(), toSave.getResultC2Opponent(), toSave.getResultC3Opponent(), toSave.getResultC4Opponent(), toSave.getResultExtraOpponent(),
                    toSave.getFoulsC1(), toSave.getFoulsC2(), toSave.getFoulsC3(), toSave.getFoulsC4(), toSave.getFoulsExtra(),
                    toSave.getFoulsC1Opponent(), toSave.getFoulsC2Opponent(), toSave.getFoulsC3Opponent(), toSave.getFoulsC4Opponent(), toSave.getFoulsExtraOpponent()
            );
        });

        Game result = gameService.createGame(command);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getTeamId());
        assertEquals("Lakers", result.getOpponent());
        assertEquals("1", result.getRound());
        assertEquals(GameStatus.PENDING, result.getStatus());
        assertEquals(0, result.getResultC1());

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        Game captured = captor.getValue();
        assertEquals(1L, captured.getTeamId());
        assertEquals("Lakers", captured.getOpponent());
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when team does not exist on create")
    void shouldThrowTeamNotFoundExceptionOnCreate() {
        CreateGameCommand command = new CreateGameCommand(
                999L, "Lakers", "1", "2026-11-01", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );

        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> gameService.createGame(command));
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("Should successfully find game by ID")
    void shouldFindGameByIdSuccessfully() {
        Game game = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game result = gameService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Lakers", result.getOpponent());
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when game not found by ID")
    void shouldThrowGameNotFoundExceptionWhenNotFound() {
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> gameService.findById(999L));
    }

    @Test
    @DisplayName("Should return all games")
    void shouldReturnAllGames() {
        Game game = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(gameRepository.findAll()).thenReturn(List.of(game));

        List<Game> results = gameService.findAll();

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    @DisplayName("Should return games by teamId")
    void shouldReturnGamesByTeamId() {
        Game game = Game.reconstitute(
                1L, 2L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(gameRepository.findByTeamId(2L)).thenReturn(List.of(game));

        List<Game> results = gameService.findByTeamId(2L);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getTeamId());
    }

    @Test
    @DisplayName("Should update game successfully when game and team exist")
    void shouldUpdateGameSuccessfully() {
        Game existingGame = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        UpdateGameCommand command = new UpdateGameCommand(
                1L, 2L, "Clippers", "2", "2026-11-02", "finished",
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0
        );

        when(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame));
        when(teamRepository.existsById(2L)).thenReturn(true);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game updated = gameService.updateGame(command);

        assertNotNull(updated);
        assertEquals(2L, updated.getTeamId());
        assertEquals("Clippers", updated.getOpponent());
        assertEquals("2", updated.getRound());
        assertEquals(GameStatus.FINISHED, updated.getStatus());
        assertEquals(20, updated.getResultC1());
        assertEquals(3, updated.getFoulsC1());
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when updating non-existent game")
    void shouldThrowGameNotFoundExceptionOnUpdate() {
        UpdateGameCommand command = new UpdateGameCommand(
                999L, 2L, "Clippers", "2", "2026-11-02", "finished",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> gameService.updateGame(command));
        verify(teamRepository, never()).existsById(any());
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when updating game with non-existent team")
    void shouldThrowTeamNotFoundExceptionOnUpdate() {
        Game existingGame = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        UpdateGameCommand command = new UpdateGameCommand(
                1L, 999L, "Clippers", "2", "2026-11-02", "finished",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame));
        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> gameService.updateGame(command));
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully delete game by ID when it exists")
    void shouldDeleteGameSuccessfully() {
        when(gameRepository.existsById(1L)).thenReturn(true);

        gameService.deleteGame(1L);

        verify(gameRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when deleting non-existent game")
    void shouldThrowGameNotFoundExceptionOnDelete() {
        when(gameRepository.existsById(999L)).thenReturn(false);

        assertThrows(GameNotFoundException.class, () -> gameService.deleteGame(999L));
        verify(gameRepository, never()).deleteById(any());
    }
}
