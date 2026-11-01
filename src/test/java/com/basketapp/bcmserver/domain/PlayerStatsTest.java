package com.basketapp.bcmserver.domain;

import com.basketapp.bcmserver.domain.model.PlayerStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerStatsTest {

    @Test
    @DisplayName("Should create valid new PlayerStats with null id and default zeros for null counters")
    void shouldCreateValidNewPlayerStatsWithDefaults() {
        PlayerStats stats = PlayerStats.create(1L, 2L, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        assertNull(stats.getId());
        assertEquals(1L, stats.getGameId());
        assertEquals(2L, stats.getPlayerId());
        assertEquals(0, stats.getMinutes());
        assertEquals(0, stats.getSeconds());
        assertEquals(0, stats.getT1a());
        assertEquals(0, stats.getT1i());
        assertEquals(0, stats.getT2a());
        assertEquals(0, stats.getT2i());
        assertEquals(0, stats.getT3a());
        assertEquals(0, stats.getT3i());
        assertEquals(0, stats.getOreb());
        assertEquals(0, stats.getDreb());
        assertEquals(0, stats.getAsis());
        assertEquals(0, stats.getRec());
        assertEquals(0, stats.getPer());
        assertEquals(0, stats.getFalt());
        assertEquals(0, stats.getValuation());
    }

    @Test
    @DisplayName("Should reconstitute existing PlayerStats with all persistent fields")
    void shouldReconstituteExistingPlayerStats() {
        PlayerStats stats = PlayerStats.reconstitute(10L, 1L, 2L, 25, 45, 5, 4, 10, 6, 4, 2,
                3, 5, 7, 2, 1, 3, 24);

        assertEquals(10L, stats.getId());
        assertEquals(1L, stats.getGameId());
        assertEquals(2L, stats.getPlayerId());
        assertEquals(25, stats.getMinutes());
        assertEquals(45, stats.getSeconds());
        assertEquals(5, stats.getT1a());
        assertEquals(4, stats.getT1i());
        assertEquals(10, stats.getT2a());
        assertEquals(6, stats.getT2i());
        assertEquals(4, stats.getT3a());
        assertEquals(2, stats.getT3i());
        assertEquals(3, stats.getOreb());
        assertEquals(5, stats.getDreb());
        assertEquals(7, stats.getAsis());
        assertEquals(2, stats.getRec());
        assertEquals(1, stats.getPer());
        assertEquals(3, stats.getFalt());
        assertEquals(24, stats.getValuation());
    }

    @Test
    @DisplayName("Should accept negative valuation score")
    void shouldAcceptNegativeValuation() {
        PlayerStats stats = PlayerStats.create(1L, 2L, 10, 0, 0, 0, 2, 0, 1, 0,
                0, 0, 0, 0, 4, 5, -9);

        assertEquals(-9, stats.getValuation());
    }

    @Test
    @DisplayName("Should reject null ID on reconstitution")
    void shouldRejectNullIdOnReconstitution() {
        assertThrows(NullPointerException.class, () ->
                PlayerStats.reconstitute(null, 1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Should reject non-positive ID on reconstitution")
    void shouldRejectNonPositiveIdOnReconstitution(Long invalidId) {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.reconstitute(invalidId, 1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Should reject non-positive gameId")
    void shouldRejectNonPositiveGameId(Long invalidGameId) {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(invalidGameId, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject null gameId")
    void shouldRejectNullGameId() {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(null, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Should reject non-positive playerId")
    void shouldRejectNonPositivePlayerId(Long invalidPlayerId) {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, invalidPlayerId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject null playerId")
    void shouldRejectNullPlayerId() {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject negative minutes")
    void shouldRejectNegativeMinutes() {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, 2L, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 60, 100})
    @DisplayName("Should reject invalid seconds outside 0..59")
    void shouldRejectInvalidSeconds(int invalidSeconds) {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, 2L, 10, invalidSeconds, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should reject made shots exceeding attempted shots")
    void shouldRejectMadeShotsExceedingAttempted() {
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, 2L, 10, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, 2L, 10, 0, 0, 0, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                PlayerStats.create(1L, 2L, 10, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("Should update player stats and validate invariants")
    void shouldUpdatePlayerStats() {
        PlayerStats stats = PlayerStats.create(1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        stats.update(3L, 4L, 30, 20, 5, 5, 8, 4, 3, 1, 2, 6, 8, 3, 2, 4, 18);

        assertEquals(3L, stats.getGameId());
        assertEquals(4L, stats.getPlayerId());
        assertEquals(30, stats.getMinutes());
        assertEquals(20, stats.getSeconds());
        assertEquals(5, stats.getT1a());
        assertEquals(5, stats.getT1i());
        assertEquals(8, stats.getT2a());
        assertEquals(4, stats.getT2i());
        assertEquals(3, stats.getT3a());
        assertEquals(1, stats.getT3i());
        assertEquals(2, stats.getOreb());
        assertEquals(6, stats.getDreb());
        assertEquals(8, stats.getAsis());
        assertEquals(3, stats.getRec());
        assertEquals(2, stats.getPer());
        assertEquals(4, stats.getFalt());
        assertEquals(18, stats.getValuation());
    }

    @Test
    @DisplayName("Should verify equals and hashCode based on ID")
    void shouldVerifyEqualsAndHashCode() {
        PlayerStats s1 = PlayerStats.reconstitute(1L, 1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStats s2 = PlayerStats.reconstitute(1L, 5L, 6L, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStats s3 = PlayerStats.reconstitute(2L, 1L, 2L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }
}

