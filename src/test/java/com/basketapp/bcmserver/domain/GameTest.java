package com.basketapp.bcmserver.domain;

import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    @DisplayName("Should create valid new Game with default values for null fields")
    void shouldCreateValidNewGameWithDefaults() {
        Game game = Game.create(
                1L,
                "  Lakers  ",
                null, // default "1"
                "  2026-11-01  ",
                null, // default PENDING
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );

        assertNull(game.getId());
        assertEquals(1L, game.getTeamId());
        assertEquals("Lakers", game.getOpponent());
        assertEquals("1", game.getRound());
        assertEquals("2026-11-01", game.getDate());
        assertEquals(GameStatus.PENDING, game.getStatus());
        assertNotNull(game.getCreatedAt());

        assertEquals(0, game.getResultC1());
        assertEquals(0, game.getResultC2());
        assertEquals(0, game.getResultC3());
        assertEquals(0, game.getResultC4());
        assertEquals(0, game.getResultExtra());
        assertEquals(0, game.getResultC1Opponent());
        assertEquals(0, game.getResultC2Opponent());
        assertEquals(0, game.getResultC3Opponent());
        assertEquals(0, game.getResultC4Opponent());
        assertEquals(0, game.getResultExtraOpponent());

        assertEquals(0, game.getFoulsC1());
        assertEquals(0, game.getFoulsC2());
        assertEquals(0, game.getFoulsC3());
        assertEquals(0, game.getFoulsC4());
        assertEquals(0, game.getFoulsExtra());
        assertEquals(0, game.getFoulsC1Opponent());
        assertEquals(0, game.getFoulsC2Opponent());
        assertEquals(0, game.getFoulsC3Opponent());
        assertEquals(0, game.getFoulsC4Opponent());
        assertEquals(0, game.getFoulsExtraOpponent());
    }

    @Test
    @DisplayName("Should reconstitute existing Game with all fields")
    void shouldReconstituteExistingGame() {
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                10L, 2L, "Celtics", "Finals", "2026-11-15", GameStatus.LIVE, now,
                25, 20, 22, 28, 0,
                18, 24, 21, 26, 0,
                4, 3, 5, 2, 0,
                3, 4, 2, 5, 0
        );

        assertEquals(10L, game.getId());
        assertEquals(2L, game.getTeamId());
        assertEquals("Celtics", game.getOpponent());
        assertEquals("Finals", game.getRound());
        assertEquals("2026-11-15", game.getDate());
        assertEquals(GameStatus.LIVE, game.getStatus());
        assertEquals(now, game.getCreatedAt());
        assertEquals(25, game.getResultC1());
        assertEquals(20, game.getResultC2());
        assertEquals(22, game.getResultC3());
        assertEquals(28, game.getResultC4());
        assertEquals(0, game.getResultExtra());
        assertEquals(18, game.getResultC1Opponent());
        assertEquals(24, game.getResultC2Opponent());
        assertEquals(21, game.getResultC3Opponent());
        assertEquals(26, game.getResultC4Opponent());
        assertEquals(0, game.getResultExtraOpponent());
        assertEquals(4, game.getFoulsC1());
        assertEquals(3, game.getFoulsC2());
        assertEquals(5, game.getFoulsC3());
        assertEquals(2, game.getFoulsC4());
        assertEquals(0, game.getFoulsExtra());
        assertEquals(3, game.getFoulsC1Opponent());
        assertEquals(4, game.getFoulsC2Opponent());
        assertEquals(2, game.getFoulsC3Opponent());
        assertEquals(5, game.getFoulsC4Opponent());
        assertEquals(0, game.getFoulsExtraOpponent());
    }

    @Test
    @DisplayName("Should reject null or non-positive ID on reconstitution")
    void shouldRejectInvalidIdOnReconstitution() {
        assertThrows(NullPointerException.class, () ->
                Game.reconstitute(null, 1L, "Warriors", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                Game.reconstitute(-1L, 1L, "Warriors", "1", "2026-11-01", GameStatus.PENDING, Instant.now(),
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -50L})
    @DisplayName("Should reject non-positive teamId")
    void shouldRejectNonPositiveTeamId(Long invalidTeamId) {
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(invalidTeamId, "Bulls", "1", "2026-11-01", GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject null teamId")
    void shouldRejectNullTeamId() {
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(null, "Bulls", "1", "2026-11-01", GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null or blank opponent")
    void shouldRejectBlankOpponent(String invalidOpponent) {
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(1L, invalidOpponent, "1", "2026-11-01", GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject opponent longer than 255 characters")
    void shouldRejectOverlongOpponent() {
        String longOpponent = "A".repeat(256);
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(1L, longOpponent, "1", "2026-11-01", GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should reject null or blank date")
    void shouldRejectBlankDate(String invalidDate) {
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(1L, "Suns", "1", invalidDate, GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject negative scores and fouls")
    void shouldRejectNegativeScoresAndFouls() {
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(1L, "Heat", "1", "2026-11-01", GameStatus.PENDING,
                        -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                Game.create(1L, "Heat", "1", "2026-11-01", GameStatus.PENDING,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should update game properties correctly")
    void shouldUpdateGame() {
        Game game = Game.create(1L, "Spurs", "1", "2026-11-01", GameStatus.PENDING,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        game.update(2L, "Clippers", "2", "2026-11-02", GameStatus.FINISHED,
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0);

        assertEquals(2L, game.getTeamId());
        assertEquals("Clippers", game.getOpponent());
        assertEquals("2", game.getRound());
        assertEquals("2026-11-02", game.getDate());
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(20, game.getResultC1());
        assertEquals(18, game.getResultC1Opponent());
        assertEquals(3, game.getFoulsC1());
        assertEquals(2, game.getFoulsC1Opponent());
    }

    @Test
    @DisplayName("Should verify equals and hashCode by ID")
    void shouldVerifyEqualsAndHashCode() {
        Game game1 = Game.reconstitute(1L, 1L, "A", "1", "2026-01-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Game game2 = Game.reconstitute(1L, 2L, "B", "2", "2026-02-02", GameStatus.LIVE, Instant.now(),
                10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Game game3 = Game.reconstitute(2L, 1L, "A", "1", "2026-01-01", GameStatus.PENDING, Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertEquals(game1, game2);
        assertEquals(game1.hashCode(), game2.hashCode());
        assertNotEquals(game1, game3);
    }
}

