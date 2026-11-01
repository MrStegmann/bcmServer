package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerPersistenceMapper;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerPersistenceMapperImpl;
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
class PlayerRepositoryAdapterTest {

    @Mock
    private SpringDataPlayerRepository jpaRepository;

    private PlayerPersistenceMapper mapper;
    private PlayerRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new PlayerPersistenceMapperImpl();
        adapter = new PlayerRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Should map and persist domain Player via JPA repository")
    void shouldSavePlayer() {
        Player player = Player.create(1L, "Michael", "Jordan", "23", "JORDAN");
        PlayerJpaEntity savedEntity = new PlayerJpaEntity(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());

        when(jpaRepository.save(any(PlayerJpaEntity.class))).thenReturn(savedEntity);

        Player result = adapter.save(player);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getTeamId());
        assertEquals("Michael", result.getFirstName());
        assertEquals("Jordan", result.getLastName());
        assertEquals("23", result.getNumber());
        assertEquals("JORDAN", result.getJerseyName());
        verify(jpaRepository).save(any(PlayerJpaEntity.class));
    }

    @Test
    @DisplayName("Should find player by ID and map to domain model")
    void shouldFindById() {
        PlayerJpaEntity entity = new PlayerJpaEntity(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<Player> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("Michael", result.get().getFirstName());
        assertEquals("23", result.get().getNumber());
    }

    @Test
    @DisplayName("Should find all players and map to domain models")
    void shouldFindAll() {
        PlayerJpaEntity entity1 = new PlayerJpaEntity(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerJpaEntity entity2 = new PlayerJpaEntity(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN", Instant.now());
        when(jpaRepository.findAll()).thenReturn(List.of(entity1, entity2));

        List<Player> results = adapter.findAll();

        assertEquals(2, results.size());
        assertEquals("Michael", results.get(0).getFirstName());
        assertEquals("Scottie", results.get(1).getFirstName());
    }

    @Test
    @DisplayName("Should find players by team ID and map to domain models")
    void shouldFindByTeamId() {
        PlayerJpaEntity entity1 = new PlayerJpaEntity(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerJpaEntity entity2 = new PlayerJpaEntity(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN", Instant.now());
        when(jpaRepository.findByTeamId(1L)).thenReturn(List.of(entity1, entity2));

        List<Player> results = adapter.findByTeamId(1L);

        assertEquals(2, results.size());
        assertEquals("Michael", results.get(0).getFirstName());
        assertEquals("Scottie", results.get(1).getFirstName());
        verify(jpaRepository).findByTeamId(1L);
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

