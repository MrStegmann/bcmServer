package com.basketapp.bcmserver.domain;

import com.basketapp.bcmserver.domain.model.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerTest {

    @Test
    @DisplayName("Should create valid new Player with null id, trimmed fields and generated createdAt")
    void shouldCreateValidNewPlayer() {
        Player player = Player.create(1L, "  Michael  ", "  Jordan  ", "  23  ", "  AIR JORDAN  ");

        assertNull(player.getId());
        assertEquals(1L, player.getTeamId());
        assertEquals("Michael", player.getFirstName());
        assertEquals("Jordan", player.getLastName());
        assertEquals("23", player.getNumber());
        assertEquals("AIR JORDAN", player.getJerseyName());
        assertNotNull(player.getCreatedAt());
    }

    @Test
    @DisplayName("Should normalize blank or null jerseyName to null")
    void shouldNormalizeBlankOrNullJerseyNameToNull() {
        Player playerNull = Player.create(1L, "Kobe", "Bryant", "24", null);
        assertNull(playerNull.getJerseyName());

        Player playerBlank = Player.create(1L, "Kobe", "Bryant", "24", "   ");
        assertNull(playerBlank.getJerseyName());
    }

    @Test
    @DisplayName("Should reconstitute existing Player with all persistent fields")
    void shouldReconstituteExistingPlayer() {
        Instant now = Instant.now();
        Player player = Player.reconstitute(10L, 2L, "Stephen", "Curry", "30", "CURRY", now);

        assertEquals(10L, player.getId());
        assertEquals(2L, player.getTeamId());
        assertEquals("Stephen", player.getFirstName());
        assertEquals("Curry", player.getLastName());
        assertEquals("30", player.getNumber());
        assertEquals("CURRY", player.getJerseyName());
        assertEquals(now, player.getCreatedAt());
    }

    @Test
    @DisplayName("Should reject null ID on reconstitution")
    void shouldRejectNullIdOnReconstitution() {
        assertThrows(NullPointerException.class, () ->
                Player.reconstitute(null, 1L, "LeBron", "James", "6", "KING JAMES", Instant.now())
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Should reject non-positive teamId")
    void shouldRejectNonPositiveTeamId(Long invalidTeamId) {
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(invalidTeamId, "LeBron", "James", "23", "JAMES")
        );
    }

    @Test
    @DisplayName("Should reject null teamId")
    void shouldRejectNullTeamId() {
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(null, "LeBron", "James", "23", "JAMES")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject invalid firstName on create")
    void shouldRejectInvalidFirstName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, invalidName, "Jordan", "23", null)
        );
    }

    @Test
    @DisplayName("Should reject firstName exceeding 100 characters")
    void shouldRejectFirstNameExceeding100Characters() {
        String longName = "A".repeat(101);
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, longName, "Jordan", "23", null)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject invalid lastName on create")
    void shouldRejectInvalidLastName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, "Michael", invalidName, "23", null)
        );
    }

    @Test
    @DisplayName("Should reject lastName exceeding 100 characters")
    void shouldRejectLastNameExceeding100Characters() {
        String longName = "B".repeat(101);
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, "Michael", longName, "23", null)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject invalid jersey number on create")
    void shouldRejectInvalidNumber(String invalidNumber) {
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, "Michael", "Jordan", invalidNumber, null)
        );
    }

    @Test
    @DisplayName("Should reject jersey number exceeding 10 characters")
    void shouldRejectNumberExceeding10Characters() {
        String longNumber = "1".repeat(11);
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, "Michael", "Jordan", longNumber, null)
        );
    }

    @Test
    @DisplayName("Should reject jerseyName exceeding 50 characters")
    void shouldRejectJerseyNameExceeding50Characters() {
        String longJerseyName = "C".repeat(51);
        assertThrows(IllegalArgumentException.class, () ->
                Player.create(1L, "Michael", "Jordan", "23", longJerseyName)
        );
    }

    @Test
    @DisplayName("Should update player attributes and validate invariants")
    void shouldUpdatePlayer() {
        Player player = Player.create(1L, "Michael", "Jordan", "23", "JORDAN");
        player.update(2L, "  Michael  ", "  Jordan  ", "  45  ", "  COMEBACK  ");

        assertEquals(2L, player.getTeamId());
        assertEquals("Michael", player.getFirstName());
        assertEquals("Jordan", player.getLastName());
        assertEquals("45", player.getNumber());
        assertEquals("COMEBACK", player.getJerseyName());
    }

    @Test
    @DisplayName("Should verify equals and hashCode based on ID")
    void shouldVerifyEqualsAndHashCode() {
        Instant now = Instant.now();
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", now);
        Player p2 = Player.reconstitute(1L, 2L, "Mike", "J", "45", "MJ", now);
        Player p3 = Player.reconstitute(2L, 1L, "Michael", "Jordan", "23", "JORDAN", now);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }
}

