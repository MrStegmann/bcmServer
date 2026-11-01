package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.domain.model.Team;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.TeamPersistenceMapper;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.TeamPersistenceMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamRepositoryAdapterTest {

    @Mock
    private SpringDataTeamRepository jpaRepository;

    private TeamPersistenceMapper mapper;
    private TeamRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new TeamPersistenceMapperImpl();
        adapter = new TeamRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Should map and persist domain Team via JPA repository")
    void shouldSaveTeam() {
        Team team = Team.create("Lakers", "Senior A");
        TeamJpaEntity savedEntity = new TeamJpaEntity(1L, "Lakers", "Senior A", Instant.now());

        when(jpaRepository.save(any(TeamJpaEntity.class))).thenReturn(savedEntity);

        Team result = adapter.save(team);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Lakers", result.getName());
        assertEquals("Senior A", result.getCategory());
        verify(jpaRepository).save(any(TeamJpaEntity.class));
    }

    @Test
    @DisplayName("Should find team by ID and map to domain model")
    void shouldFindById() {
        TeamJpaEntity entity = new TeamJpaEntity(1L, "Lakers", "Senior A", Instant.now());
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<Team> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("Lakers", result.get().getName());
        assertEquals("Senior A", result.get().getCategory());
    }

    @Test
    @DisplayName("Should find all teams and map to domain models")
    void shouldFindAll() {
        TeamJpaEntity entity1 = new TeamJpaEntity(1L, "Lakers", "Senior A", Instant.now());
        TeamJpaEntity entity2 = new TeamJpaEntity(2L, "Bulls", "Senior B", Instant.now());
        when(jpaRepository.findAll()).thenReturn(List.of(entity1, entity2));

        List<Team> results = adapter.findAll();

        assertEquals(2, results.size());
        assertEquals("Lakers", results.get(0).getName());
        assertEquals("Bulls", results.get(1).getName());
    }

    @Test
    @DisplayName("Should delegate existsById check")
    void shouldCheckExistsById() {
        when(jpaRepository.existsById(1L)).thenReturn(true);
        when(jpaRepository.existsById(2L)).thenReturn(false);

        assertTrue(adapter.existsById(1L));
        assertFalse(adapter.existsById(2L));
        verify(jpaRepository).existsById(1L);
        verify(jpaRepository).existsById(2L);
    }

    @Test
    @DisplayName("Should delegate deleteById")
    void shouldDeleteById() {
        adapter.deleteById(1L);

        verify(jpaRepository).deleteById(1L);
    }
}

