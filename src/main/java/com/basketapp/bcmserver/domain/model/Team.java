package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Pure Java 21 Team domain entity.
 * Free from framework and persistence annotations.
 */
public class Team {

    private final Long id;
    private String name;
    private String category;
    private final Instant createdAt;

    public Team(Long id, String name, String category, Instant createdAt) {
        this.id = id;
        this.name = sanitizeAndValidateName(name);
        this.category = sanitizeCategory(category);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Team create(String name, String category) {
        return new Team(null, name, category, Instant.now());
    }

    public static Team reconstitute(Long id, String name, String category, Instant createdAt) {
        Objects.requireNonNull(id, "ID cannot be null when reconstituting existing team");
        return new Team(id, name, category, createdAt);
    }

    public void update(String name, String category) {
        this.name = sanitizeAndValidateName(name);
        this.category = sanitizeCategory(category);
    }

    private static String sanitizeAndValidateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be null, empty, or blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Team name cannot exceed 100 characters");
        }
        return trimmed;
    }

    private static String sanitizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String trimmed = category.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("Team category cannot exceed 50 characters");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return Objects.equals(id, team.id) && Objects.equals(name, team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

