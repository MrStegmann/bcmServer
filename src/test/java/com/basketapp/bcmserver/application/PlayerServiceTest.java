package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.CreatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerCommand;
import com.basketapp.bcmserver.application.port.out.PlayerRepositoryPort;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.application.service.PlayerService;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Player;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepositoryPort playerRepository;

    @Mock
    private TeamRepositoryPort teamRepository;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, teamRepository);
    }

    @Test
    @DisplayName("Should successfully create a new player when team exists")
    void shouldCreatePlayerSuccessfully() {
        CreatePlayerCommand command = new CreatePlayerCommand(1L, "Michael", "Jordan", "23", "JORDAN");
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player toSave = invocation.getArgument(0);
            return Player.reconstitute(10L, toSave.getTeamId(), toSave.getFirstName(), toSave.getLastName(), toSave.getNumber(), toSave.getJerseyName(), Instant.now());
        });

        Player result = playerService.createPlayer(command);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getTeamId());
        assertEquals("Michael", result.getFirstName());
        assertEquals("Jordan", result.getLastName());
        assertEquals("23", result.getNumber());
        assertEquals("JORDAN", result.getJerseyName());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getTeamId());
        assertEquals("Michael", captor.getValue().getFirstName());
        assertEquals("Jordan", captor.getValue().getLastName());
        assertEquals("23", captor.getValue().getNumber());
        assertEquals("JORDAN", captor.getValue().getJerseyName());
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when creating player for non-existent team")
    void shouldThrowTeamNotFoundExceptionWhenCreatingPlayerForNonExistentTeam() {
        CreatePlayerCommand command = new CreatePlayerCommand(999L, "Michael", "Jordan", "23", "JORDAN");
        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> playerService.createPlayer(command));

        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should normalize blank jerseyName to null upon player creation")
    void shouldNormalizeBlankJerseyNameToNullUponCreation() {
        CreatePlayerCommand command = new CreatePlayerCommand(1L, "Michael", "Jordan", "23", "   ");
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Player result = playerService.createPlayer(command);

        assertNull(result.getJerseyName());
    }

    @Test
    @DisplayName("Should find player by ID when player exists")
    void shouldFindPlayerByIdSuccessfully() {
        Player player = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        Player result = playerService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Michael", result.getFirstName());
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when player does not exist")
    void shouldThrowPlayerNotFoundExceptionWhenPlayerDoesNotExist() {
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> playerService.findById(999L));
    }

    @Test
    @DisplayName("Should retrieve all players")
    void shouldFindAllPlayers() {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        Player p2 = Player.reconstitute(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN", Instant.now());
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Player> results = playerService.findAll();

        assertEquals(2, results.size());
        assertEquals("Michael", results.get(0).getFirstName());
        assertEquals("Scottie", results.get(1).getFirstName());
    }

    @Test
    @DisplayName("Should retrieve players filtered by team ID")
    void shouldFindPlayersByTeamId() {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        when(playerRepository.findByTeamId(1L)).thenReturn(List.of(p1));

        List<Player> results = playerService.findByTeamId(1L);

        assertEquals(1, results.size());
        assertEquals("Michael", results.get(0).getFirstName());
        verify(playerRepository).findByTeamId(1L);
    }

    @Test
    @DisplayName("Should successfully update player attributes when player and team exist")
    void shouldUpdatePlayerSuccessfully() {
        UpdatePlayerCommand command = new UpdatePlayerCommand(1L, 2L, "Michael", "Jordan", "45", "COMEBACK");
        Player existing = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(2L)).thenReturn(true);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Player result = playerService.updatePlayer(command);

        assertNotNull(result);
        assertEquals(2L, result.getTeamId());
        assertEquals("45", result.getNumber());
        assertEquals("COMEBACK", result.getJerseyName());
        verify(playerRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when updating non-existent player")
    void shouldThrowPlayerNotFoundExceptionWhenUpdatingNonExistentPlayer() {
        UpdatePlayerCommand command = new UpdatePlayerCommand(999L, 1L, "Michael", "Jordan", "23", "JORDAN");
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> playerService.updatePlayer(command));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when updating player with non-existent team")
    void shouldThrowTeamNotFoundExceptionWhenUpdatingPlayerWithNonExistentTeam() {
        UpdatePlayerCommand command = new UpdatePlayerCommand(1L, 999L, "Michael", "Jordan", "23", "JORDAN");
        Player existing = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> playerService.updatePlayer(command));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should successfully delete player when player exists")
    void shouldDeletePlayerSuccessfullyWhenPlayerExists() {
        when(playerRepository.existsById(1L)).thenReturn(true);

        playerService.deletePlayer(1L);

        verify(playerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when deleting non-existent player")
    void shouldThrowPlayerNotFoundExceptionWhenDeletingNonExistentPlayer() {
        when(playerRepository.existsById(999L)).thenReturn(false);

        assertThrows(PlayerNotFoundException.class, () -> playerService.deletePlayer(999L));
        verify(playerRepository, never()).deleteById(any());
    }
}

