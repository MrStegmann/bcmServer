package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.out.GameRepositoryPort;
import com.basketapp.bcmserver.application.port.out.PlayerRepositoryPort;
import com.basketapp.bcmserver.application.port.out.PlayerStatsRepositoryPort;
import com.basketapp.bcmserver.application.service.PlayerStatsService;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerStatsNotFoundException;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock
    private PlayerStatsRepositoryPort playerStatsRepository;

    @Mock
    private GameRepositoryPort gameRepository;

    @Mock
    private PlayerRepositoryPort playerRepository;

    private PlayerStatsService playerStatsService;

    @BeforeEach
    void setUp() {
        playerStatsService = new PlayerStatsService(playerStatsRepository, gameRepository, playerRepository);
    }

    @Test
    @DisplayName("Should successfully create player stats when game and player exist")
    void shouldCreatePlayerStatsSuccessfully() {
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                10L, 4L, 28, 45, 6, 5, 10, 7, 5, 2, 2, 6, 8, 3, 2, 3, 24
        );

        when(gameRepository.existsById(10L)).thenReturn(true);
        when(playerRepository.existsById(4L)).thenReturn(true);
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> {
            PlayerStats toSave = invocation.getArgument(0);
            return PlayerStats.reconstitute(
                    1L, toSave.getGameId(), toSave.getPlayerId(), toSave.getMinutes(), toSave.getSeconds(),
                    toSave.getT1a(), toSave.getT1i(), toSave.getT2a(), toSave.getT2i(), toSave.getT3a(), toSave.getT3i(),
                    toSave.getOreb(), toSave.getDreb(), toSave.getAsis(), toSave.getRec(), toSave.getPer(),
                    toSave.getFalt(), toSave.getValuation()
            );
        });

        PlayerStats result = playerStatsService.createPlayerStats(command);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getGameId());
        assertEquals(4L, result.getPlayerId());
        assertEquals(28, result.getMinutes());
        assertEquals(24, result.getValuation());

        ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
        verify(playerStatsRepository).save(captor.capture());
        PlayerStats captured = captor.getValue();
        assertEquals(10L, captured.getGameId());
        assertEquals(4L, captured.getPlayerId());
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when target game does not exist")
    void shouldThrowGameNotFoundExceptionWhenGameMissing() {
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                999L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameRepository.existsById(999L)).thenReturn(false);

        assertThrows(GameNotFoundException.class, () -> playerStatsService.createPlayerStats(command));
        verify(playerStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when target player does not exist")
    void shouldThrowPlayerNotFoundExceptionWhenPlayerMissing() {
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                10L, 999L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameRepository.existsById(10L)).thenReturn(true);
        when(playerRepository.existsById(999L)).thenReturn(false);

        assertThrows(PlayerNotFoundException.class, () -> playerStatsService.createPlayerStats(command));
        verify(playerStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find player stats by ID successfully")
    void shouldFindPlayerStatsByIdSuccessfully() {
        PlayerStats stats = PlayerStats.reconstitute(
                1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(playerStatsRepository.findById(1L)).thenReturn(Optional.of(stats));

        PlayerStats result = playerStatsService.getPlayerStatsById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getGameId());
        assertEquals(4L, result.getPlayerId());
    }

    @Test
    @DisplayName("Should throw PlayerStatsNotFoundException when stats record missing by ID")
    void shouldThrowPlayerStatsNotFoundExceptionWhenNotFound() {
        when(playerStatsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PlayerStatsNotFoundException.class, () -> playerStatsService.getPlayerStatsById(999L));
    }

    @Test
    @DisplayName("Should return all player stats")
    void shouldReturnAllPlayerStats() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStats s2 = PlayerStats.reconstitute(2L, 10L, 5L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(playerStatsRepository.findAll()).thenReturn(List.of(s1, s2));

        List<PlayerStats> results = playerStatsService.getAllPlayerStats();

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should filter player stats by gameId and playerId")
    void shouldFilterPlayerStatsByGameAndPlayer() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(playerStatsRepository.findByGameIdAndPlayerId(10L, 4L)).thenReturn(List.of(s1));

        List<PlayerStats> results = playerStatsService.getPlayerStatsByFilters(10L, 4L);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getGameId());
        assertEquals(4L, results.get(0).getPlayerId());
    }

    @Test
    @DisplayName("Should filter player stats by gameId only")
    void shouldFilterPlayerStatsByGameOnly() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(playerStatsRepository.findByGameId(10L)).thenReturn(List.of(s1));

        List<PlayerStats> results = playerStatsService.getPlayerStatsByFilters(10L, null);

        assertEquals(1, results.size());
        verify(playerStatsRepository).findByGameId(10L);
    }

    @Test
    @DisplayName("Should filter player stats by playerId only")
    void shouldFilterPlayerStatsByPlayerOnly() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(playerStatsRepository.findByPlayerId(4L)).thenReturn(List.of(s1));

        List<PlayerStats> results = playerStatsService.getPlayerStatsByFilters(null, 4L);

        assertEquals(1, results.size());
        verify(playerStatsRepository).findByPlayerId(4L);
    }

    @Test
    @DisplayName("Should return all when both filters are null")
    void shouldReturnAllWhenBothFiltersNull() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(playerStatsRepository.findAll()).thenReturn(List.of(s1));

        List<PlayerStats> results = playerStatsService.getPlayerStatsByFilters(null, null);

        assertEquals(1, results.size());
        verify(playerStatsRepository).findAll();
    }

    @Test
    @DisplayName("Should update player stats successfully when target record, game, and player exist")
    void shouldUpdatePlayerStatsSuccessfully() {
        PlayerStats existing = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                1L, 11L, 5L, 32, 15, 4, 3, 8, 5, 6, 3, 3, 5, 7, 2, 1, 4, 25
        );

        when(playerStatsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.existsById(11L)).thenReturn(true);
        when(playerRepository.existsById(5L)).thenReturn(true);
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerStats updated = playerStatsService.updatePlayerStats(command);

        assertNotNull(updated);
        assertEquals(1L, updated.getId());
        assertEquals(11L, updated.getGameId());
        assertEquals(5L, updated.getPlayerId());
        assertEquals(32, updated.getMinutes());
        assertEquals(15, updated.getSeconds());
        assertEquals(25, updated.getValuation());
        verify(playerStatsRepository).save(any(PlayerStats.class));
    }

    @Test
    @DisplayName("Should throw PlayerStatsNotFoundException when updating non-existent stats record")
    void shouldThrowPlayerStatsNotFoundExceptionOnUpdate() {
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                999L, 10L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(playerStatsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PlayerStatsNotFoundException.class, () -> playerStatsService.updatePlayerStats(command));
        verify(playerStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when updating stats with non-existent game")
    void shouldThrowGameNotFoundExceptionOnUpdate() {
        PlayerStats existing = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                1L, 999L, 4L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(playerStatsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.existsById(999L)).thenReturn(false);

        assertThrows(GameNotFoundException.class, () -> playerStatsService.updatePlayerStats(command));
        verify(playerStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when updating stats with non-existent player")
    void shouldThrowPlayerNotFoundExceptionOnUpdate() {
        PlayerStats existing = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                1L, 10L, 999L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(playerStatsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.existsById(10L)).thenReturn(true);
        when(playerRepository.existsById(999L)).thenReturn(false);

        assertThrows(PlayerNotFoundException.class, () -> playerStatsService.updatePlayerStats(command));
        verify(playerStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully delete player stats by ID when it exists")
    void shouldDeletePlayerStatsSuccessfully() {
        when(playerStatsRepository.existsById(1L)).thenReturn(true);

        playerStatsService.deletePlayerStats(1L);

        verify(playerStatsRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw PlayerStatsNotFoundException when deleting non-existent stats record")
    void shouldThrowPlayerStatsNotFoundExceptionOnDelete() {
        when(playerStatsRepository.existsById(999L)).thenReturn(false);

        assertThrows(PlayerStatsNotFoundException.class, () -> playerStatsService.deletePlayerStats(999L));
        verify(playerStatsRepository, never()).deleteById(any());
    }
}
