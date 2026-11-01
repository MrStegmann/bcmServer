package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerStatsPersistenceMapper;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerStatsPersistenceMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class PlayerStatsRepositoryAdapterTest {

    @Mock
    private SpringDataPlayerStatsRepository jpaRepository;

    private PlayerStatsPersistenceMapper mapper;
    private PlayerStatsRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new PlayerStatsPersistenceMapperImpl();
        adapter = new PlayerStatsRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Should map and persist domain PlayerStats via JPA repository")
    void shouldSavePlayerStats() {
        PlayerStats stats = PlayerStats.create(1L, 2L, 20, 30, 2, 2, 4, 3, 2, 1, 1, 4, 5, 2, 1, 2, 15);
        PlayerStatsJpaEntity savedEntity = new PlayerStatsJpaEntity(
                1L, 1L, 2L, 20, 30, 2, 2, 4, 3, 2, 1, 1, 4, 5, 2, 1, 2, 15
        );

        when(jpaRepository.save(any(PlayerStatsJpaEntity.class))).thenReturn(savedEntity);

        PlayerStats result = adapter.save(stats);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getGameId());
        assertEquals(2L, result.getPlayerId());
        assertEquals(20, result.getMinutes());
        assertEquals(30, result.getSeconds());
        assertEquals(15, result.getValuation());
        verify(jpaRepository).save(any(PlayerStatsJpaEntity.class));
    }

    @Test
    @DisplayName("Should find player stats by ID and map to domain model")
    void shouldFindById() {
        PlayerStatsJpaEntity entity = new PlayerStatsJpaEntity(
                1L, 1L, 2L, 20, 30, 2, 2, 4, 3, 2, 1, 1, 4, 5, 2, 1, 2, 15
        );
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<PlayerStats> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(1L, result.get().getGameId());
        assertEquals(2L, result.get().getPlayerId());
    }

    @Test
    @DisplayName("Should find all player stats and map to domain models")
    void shouldFindAll() {
        PlayerStatsJpaEntity e1 = new PlayerStatsJpaEntity(1L, 1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStatsJpaEntity e2 = new PlayerStatsJpaEntity(2L, 1L, 3L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(jpaRepository.findAll()).thenReturn(List.of(e1, e2));

        List<PlayerStats> results = adapter.findAll();

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());
    }

    @Test
    @DisplayName("Should find player stats by game ID and map to domain models")
    void shouldFindByGameId() {
        PlayerStatsJpaEntity e1 = new PlayerStatsJpaEntity(1L, 10L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(jpaRepository.findByGameId(10L)).thenReturn(List.of(e1));

        List<PlayerStats> results = adapter.findByGameId(10L);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getGameId());
        verify(jpaRepository).findByGameId(10L);
    }

    @Test
    @DisplayName("Should find player stats by player ID and map to domain models")
    void shouldFindByPlayerId() {
        PlayerStatsJpaEntity e1 = new PlayerStatsJpaEntity(1L, 10L, 20L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(jpaRepository.findByPlayerId(20L)).thenReturn(List.of(e1));

        List<PlayerStats> results = adapter.findByPlayerId(20L);

        assertEquals(1, results.size());
        assertEquals(20L, results.get(0).getPlayerId());
        verify(jpaRepository).findByPlayerId(20L);
    }

    @Test
    @DisplayName("Should find player stats by game ID and player ID")
    void shouldFindByGameIdAndPlayerId() {
        PlayerStatsJpaEntity e1 = new PlayerStatsJpaEntity(1L, 10L, 20L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(jpaRepository.findByGameIdAndPlayerId(10L, 20L)).thenReturn(List.of(e1));

        List<PlayerStats> results = adapter.findByGameIdAndPlayerId(10L, 20L);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getGameId());
        assertEquals(20L, results.get(0).getPlayerId());
        verify(jpaRepository).findByGameIdAndPlayerId(10L, 20L);
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

