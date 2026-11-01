package com.basketapp.bcmserver.domain;

import com.basketapp.bcmserver.domain.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTest {

    @Test
    @DisplayName("Should create valid new Team with null id, trimmed fields and generated createdAt")
    void shouldCreateValidNewTeam() {
        Team team = Team.create("  Lakers  ", "  Senior A  ");

        assertNull(team.getId());
        assertEquals("Lakers", team.getName());
        assertEquals("Senior A", team.getCategory());
        assertNotNull(team.getCreatedAt());
    }

    @Test
    @DisplayName("Should normalize blank or null category to null")
    void shouldNormalizeBlankOrNullCategoryToNull() {
        Team teamWithNull = Team.create("Bulls", null);
        assertNull(teamWithNull.getCategory());

        Team teamWithBlank = Team.create("Bulls", "   ");
        assertNull(teamWithBlank.getCategory());
    }

    @Test
    @DisplayName("Should reconstitute existing Team with all persistent fields")
    void shouldReconstituteExistingTeam() {
        Instant now = Instant.now();
        Team team = Team.reconstitute(1L, "Celtics", "Junior", now);

        assertEquals(1L, team.getId());
        assertEquals("Celtics", team.getName());
        assertEquals("Junior", team.getCategory());
        assertEquals(now, team.getCreatedAt());
    }

    @Test
    @DisplayName("Should reject null ID on reconstitution")
    void shouldRejectNullIdOnReconstitution() {
        assertThrows(NullPointerException.class, () ->
                Team.reconstitute(null, "Warriors", "Senior", Instant.now())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject invalid name on create")
    void shouldRejectInvalidName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () ->
                Team.create(invalidName, "Senior")
        );
    }

    @Test
    @DisplayName("Should reject name exceeding 100 characters")
    void shouldRejectNameExceeding100Characters() {
        String longName = "A".repeat(101);
        assertThrows(IllegalArgumentException.class, () ->
                Team.create(longName, "Senior")
        );
    }

    @Test
    @DisplayName("Should reject category exceeding 50 characters")
    void shouldRejectCategoryExceeding50Characters() {
        String longCategory = "B".repeat(51);
        assertThrows(IllegalArgumentException.class, () ->
                Team.create("Heat", longCategory)
        );
    }

    @Test
    @DisplayName("Should update team name and category")
    void shouldUpdateTeam() {
        Team team = Team.create("Warriors", "Senior");
        team.update("Golden State Warriors", "Elite");

        assertEquals("Golden State Warriors", team.getName());
        assertEquals("Elite", team.getCategory());
    }
}

