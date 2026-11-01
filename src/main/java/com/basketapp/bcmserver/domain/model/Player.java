package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Pure Java 21 Player domain entity.
 * Free from framework and persistence annotations.
 */
public class Player {

    private final Long id;
    private Long teamId;
    private String firstName;
    private String lastName;
    private String number;
    private String jerseyName;
    private final Instant createdAt;

    public Player(Long id, Long teamId, String firstName, String lastName, String number, String jerseyName, Instant createdAt) {
        this.id = id;
        this.teamId = validateTeamId(teamId);
        this.firstName = sanitizeAndValidateName(firstName, "First name");
        this.lastName = sanitizeAndValidateName(lastName, "Last name");
        this.number = sanitizeAndValidateNumber(number);
        this.jerseyName = sanitizeJerseyName(jerseyName);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Player create(Long teamId, String firstName, String lastName, String number, String jerseyName) {
        return new Player(null, teamId, firstName, lastName, number, jerseyName, Instant.now());
    }

    public static Player reconstitute(Long id, Long teamId, String firstName, String lastName, String number, String jerseyName, Instant createdAt) {
        Objects.requireNonNull(id, "ID cannot be null when reconstituting existing player");
        return new Player(id, teamId, firstName, lastName, number, jerseyName, createdAt);
    }

    public void update(Long teamId, String firstName, String lastName, String number, String jerseyName) {
        this.teamId = validateTeamId(teamId);
        this.firstName = sanitizeAndValidateName(firstName, "First name");
        this.lastName = sanitizeAndValidateName(lastName, "Last name");
        this.number = sanitizeAndValidateNumber(number);
        this.jerseyName = sanitizeJerseyName(jerseyName);
    }

    private static Long validateTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("Team ID must be a positive non-null number");
        }
        return teamId;
    }

    private static String sanitizeAndValidateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null, empty, or blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 100 characters");
        }
        return trimmed;
    }

    private static String sanitizeAndValidateNumber(String number) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Jersey number cannot be null, empty, or blank");
        }
        String trimmed = number.trim();
        if (trimmed.length() > 10) {
            throw new IllegalArgumentException("Jersey number cannot exceed 10 characters");
        }
        return trimmed;
    }

    private static String sanitizeJerseyName(String jerseyName) {
        if (jerseyName == null || jerseyName.isBlank()) {
            return null;
        }
        String trimmed = jerseyName.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("Jersey name cannot exceed 50 characters");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNumber() {
        return number;
    }

    public String getJerseyName() {
        return jerseyName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", teamId=" + teamId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", number='" + number + '\'' +
                ", jerseyName='" + jerseyName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

