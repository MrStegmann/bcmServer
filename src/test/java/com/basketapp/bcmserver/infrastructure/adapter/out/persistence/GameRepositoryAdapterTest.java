package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.GamePersistenceMapper;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.GamePersistenceMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRepositoryAdapterTest {

    @Mock
    private SpringDataGameRepository jpaRepository;

    private GamePersistenceMapper mapper;
    private GameRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new GamePersistenceMapperImpl();
        adapter = new GameRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Should map and persist domain Game via JPA repository")
    void shouldSaveGame() {
        Game game = Game.create(
                1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );
        GameJpaEntity savedEntity = new GameJpaEntity(
                10L, 1L, "Lakers", "1", "2026-11-01", "pending", Instant.now(),
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );

        when(jpaRepository.save(any(GameJpaEntity.class))).thenReturn(savedEntity);

        Game result = adapter.save(game);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getTeamId());
        assertEquals("Lakers", result.getOpponent());
        assertEquals("1", result.getRound());
        assertEquals(GameStatus.PENDING, result.getStatus());
        verify(jpaRepository).save(any(GameJpaEntity.class));
    }

    @Test
    @DisplayName("Should find game by ID and map to domain model")
    void shouldFindById() {
        GameJpaEntity entity = new GameJpaEntity(
                10L, 1L, "Lakers", "1", "2026-11-01", "pending", Instant.now(),
                20, 22, 18, 25, 0,
                19, 21, 20, 24, 0,
                2, 3, 4, 1, 0,
                3, 2, 4, 2, 0
        );
        when(jpaRepository.findById(10L)).thenReturn(Optional.of(entity));

        Optional<Game> result = adapter.findById(10L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        assertEquals(1L, result.get().getTeamId());
        assertEquals("Lakers", result.get().getOpponent());
        assertEquals(20, result.get().getResultC1());
        assertEquals(GameStatus.PENDING, result.get().getStatus());
    }

    @Test
    @DisplayName("Should return all games mapped to domain entities")
    void shouldFindAll() {
        GameJpaEntity entity = new GameJpaEntity(
                1L, 1L, "Warriors", "1", "2026-11-01", "pending", Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(jpaRepository.findAll()).thenReturn(List.of(entity));

        List<Game> result = adapter.findAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("Should find games by teamId")
    void shouldFindByTeamId() {
        GameJpaEntity entity = new GameJpaEntity(
                1L, 5L, "Bulls", "2", "2026-11-05", "live", Instant.now(),
                10, 0, 0, 0, 0, 8, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0
        );
        when(jpaRepository.findByTeamId(5L)).thenReturn(List.of(entity));

        List<Game> result = adapter.findByTeamId(5L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getTeamId());
        assertEquals(GameStatus.LIVE, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should delete game by ID")
    void shouldDeleteById() {
        adapter.deleteById(10L);
        verify(jpaRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Should check existence by ID")
    void shouldExistsById() {
        when(jpaRepository.existsById(10L)).thenReturn(true);
        assertTrue(adapter.existsById(10L));
    }
}

