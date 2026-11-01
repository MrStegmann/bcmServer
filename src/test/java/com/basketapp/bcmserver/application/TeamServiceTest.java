package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.CreateTeamCommand;
import com.basketapp.bcmserver.application.port.in.UpdateTeamCommand;
import com.basketapp.bcmserver.application.port.out.TeamRepositoryPort;
import com.basketapp.bcmserver.application.service.TeamService;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepositoryPort teamRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository);
    }

    @Test
    @DisplayName("Should successfully create a new team with valid attributes")
    void shouldCreateTeamSuccessfully() {
        CreateTeamCommand command = new CreateTeamCommand("Cadete Femenino A", "U16 Federado");
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team toSave = invocation.getArgument(0);
            return Team.reconstitute(1L, toSave.getName(), toSave.getCategory(), Instant.now());
        });

        Team result = teamService.createTeam(command);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Cadete Femenino A", result.getName());
        assertEquals("U16 Federado", result.getCategory());

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertEquals("Cadete Femenino A", captor.getValue().getName());
        assertEquals("U16 Federado", captor.getValue().getCategory());
    }

    @Test
    @DisplayName("Should create team with null category when empty or blank")
    void shouldCreateTeamWithNullCategoryWhenBlank() {
        CreateTeamCommand command = new CreateTeamCommand("Senior Masculino", "   ");
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team toSave = invocation.getArgument(0);
            return Team.reconstitute(2L, toSave.getName(), toSave.getCategory(), Instant.now());
        });

        Team result = teamService.createTeam(command);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Senior Masculino", result.getName());
        assertNull(result.getCategory());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when name is blank")
    void shouldThrowWhenNameIsBlank() {
        CreateTeamCommand command = new CreateTeamCommand("   ", "Senior");

        assertThrows(IllegalArgumentException.class, () -> teamService.createTeam(command));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when name exceeds 100 characters")
    void shouldThrowWhenNameExceeds100Chars() {
        CreateTeamCommand command = new CreateTeamCommand("A".repeat(101), "Senior");

        assertThrows(IllegalArgumentException.class, () -> teamService.createTeam(command));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully find team by ID")
    void shouldFindTeamByIdSuccessfully() {
        Team team = Team.reconstitute(1L, "Cadete A", "Junior", Instant.now());
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        Team result = teamService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Cadete A", result.getName());
        assertEquals("Junior", result.getCategory());
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when ID does not exist")
    void shouldThrowTeamNotFoundExceptionWhenIdDoesNotExist() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> teamService.findById(999L));
    }

    @Test
    @DisplayName("Should retrieve all teams")
    void shouldRetrieveAllTeams() {
        Team team1 = Team.reconstitute(1L, "Cadete A", "Junior", Instant.now());
        Team team2 = Team.reconstitute(2L, "Senior B", "Senior", Instant.now());
        when(teamRepository.findAll()).thenReturn(List.of(team1, team2));

        List<Team> results = teamService.findAll();

        assertEquals(2, results.size());
        assertEquals("Cadete A", results.get(0).getName());
        assertEquals("Senior B", results.get(1).getName());
    }

    @Test
    @DisplayName("Should return empty list when no teams exist")
    void shouldReturnEmptyListWhenNoTeamsExist() {
        when(teamRepository.findAll()).thenReturn(Collections.emptyList());

        List<Team> results = teamService.findAll();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should successfully update team")
    void shouldUpdateTeamSuccessfully() {
        UpdateTeamCommand command = new UpdateTeamCommand(1L, "Updated Lakers", "Senior Gold");
        Team existing = Team.reconstitute(1L, "Lakers", "Senior", Instant.now());

        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Team result = teamService.updateTeam(command);

        assertEquals("Updated Lakers", result.getName());
        assertEquals("Senior Gold", result.getCategory());
        verify(teamRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when updating non-existent team")
    void shouldThrowWhenUpdatingNonExistentTeam() {
        UpdateTeamCommand command = new UpdateTeamCommand(999L, "Updated Team", "Senior");
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> teamService.updateTeam(command));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updated name is blank")
    void shouldThrowWhenUpdatedNameIsBlank() {
        UpdateTeamCommand command = new UpdateTeamCommand(1L, "   ", "Senior");
        Team existing = Team.reconstitute(1L, "Lakers", "Senior", Instant.now());

        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> teamService.updateTeam(command));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully delete team")
    void shouldDeleteTeamSuccessfully() {
        when(teamRepository.existsById(1L)).thenReturn(true);

        teamService.deleteTeam(1L);

        verify(teamRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when deleting non-existent team")
    void shouldThrowWhenDeletingNonExistentTeam() {
        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThrows(TeamNotFoundException.class, () -> teamService.deleteTeam(999L));
        verify(teamRepository, never()).deleteById(any());
    }
}
