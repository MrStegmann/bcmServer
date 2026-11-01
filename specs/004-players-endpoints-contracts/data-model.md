# Data Model: Player Management

**Feature**: `004-players-endpoints-contracts`  
**Status**: Draft  
**Date**: 2026-10-31  

This document specifies the domain entity, DTOs, persistence entity, database schema, and mapping definitions for the `players` resource in `bcmServer`.

---

## 1. Relational Database Schema (`docs/data_models.md` & Flyway)

The database table `players` adheres directly to the specification in `docs/data_models.md` and will be provisioned via Flyway migration `V3__create_players_table.sql`.

### Table Definition: `players`

| Column | PostgreSQL Type | Nullable | Default | Constraints / Description |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGSERIAL` | No | Auto-increment | `PRIMARY KEY` |
| `team_id` | `BIGINT` | No | - | `FOREIGN KEY REFERENCES teams(id) ON DELETE CASCADE` |
| `first_name` | `TEXT` | No | - | Player's given name |
| `last_name` | `TEXT` | No | - | Player's family name / surname |
| `number` | `TEXT` | No | - | Player's jersey/dorsal number (e.g., "00", "0", "7", "23") |
| `jersey_name` | `TEXT` | Yes | `NULL` | Optional name printed on the player's jersey |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System creation timestamp |

### Indexes & Integrity Rules
- Primary Key: `pk_players` on `(id)`
- Foreign Key: `fk_players_team_id` on `(team_id)` referencing `teams(id)` with `ON DELETE CASCADE`
- Index: `idx_players_team_id` on `(team_id)` to ensure performant retrieval of team rosters
- Cascading: Deleting a team cascades and removes all of its players (`teams(id) ON DELETE CASCADE`). Deleting a player cascades and removes associated records in `player_stats(player_id)`.

```sql
CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    number TEXT NOT NULL,
    jersey_name TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE INDEX idx_players_team_id ON players (team_id);
```

---

## 2. Domain Model (`com.basketapp.bcmserver.domain.model`)

Per Principle I of the bcmServer constitution, domain models are pure Java 21 classes with zero framework or persistence dependencies.

### 2.1 Player (Domain Entity)

```java
package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Player {

    private final Long id;
    private final Long teamId;
    private final String firstName;
    private final String lastName;
    private final String number;
    private final String jerseyName;
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

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getNumber() { return number; }
    public String getJerseyName() { return jerseyName; }
    public Instant getCreatedAt() { return createdAt; }

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
}
```

### 2.2 Invariants & Validation Rules
- `id`: Unique identifier; null prior to persistence, non-null positive number once persisted.
- `teamId`: Mandatory, positive 64-bit integer (`> 0`). Must reference an existing `Team` entity.
- `firstName`: Mandatory, non-blank, max 100 characters. Automatically trimmed.
- `lastName`: Mandatory, non-blank, max 100 characters. Automatically trimmed.
- `number`: Mandatory, non-blank, max 10 characters. Automatically trimmed. Allows alphanumeric or numeric combinations (e.g., "00", "0", "7", "23").
- `jerseyName`: Optional/nullable. If null or blank, normalized to `null`. If non-blank, max 50 characters, automatically trimmed.
- `createdAt`: Mandatory, default current UTC instant (`Instant.now()`), immutable.

---

## 3. Application Ports & Commands

### 3.1 Application Commands (`application.port.in`)

#### CreatePlayerCommand
```java
public record CreatePlayerCommand(
    Long teamId,
    String firstName,
    String lastName,
    String number,
    String jerseyName
) {}
```

#### UpdatePlayerCommand
```java
public record UpdatePlayerCommand(
    Long id,
    Long teamId,
    String firstName,
    String lastName,
    String number,
    String jerseyName
) {}
```

### 3.2 Inbound Use Case Ports (`application.port.in`)

- `CreatePlayerUseCase`: `Player createPlayer(CreatePlayerCommand command)`
- `GetPlayerUseCase`:
  - `Player findById(Long id)`
  - `List<Player> findAll()`
  - `List<Player> findByTeamId(Long teamId)`
- `UpdatePlayerUseCase`: `Player updatePlayer(UpdatePlayerCommand command)`
- `DeletePlayerUseCase`: `void deletePlayer(Long id)`

### 3.3 Outbound Persistence Port (`application.port.out`)

```java
public interface PlayerRepositoryPort {
    Player save(Player player);
    Optional<Player> findById(Long id);
    List<Player> findAll();
    List<Player> findByTeamId(Long teamId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

---

## 4. Web Inbound & Outbound DTOs (`infrastructure.adapter.in.web.dto`)

### 4.1 CreatePlayerRequest
```java
package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a new player on a team roster")
public record CreatePlayerRequest(
    @NotNull(message = "Team ID is required")
    @Positive(message = "Team ID must be a positive number")
    @Schema(description = "ID of the associated team", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long teamId,

    @NotBlank(message = "First name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Schema(description = "Player's first name", example = "Michael", requiredMode = Schema.RequiredMode.REQUIRED)
    String firstName,

    @NotBlank(message = "Last name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Schema(description = "Player's last name", example = "Jordan", requiredMode = Schema.RequiredMode.REQUIRED)
    String lastName,

    @NotBlank(message = "Jersey number is required and cannot be blank")
    @Size(min = 1, max = 10, message = "Jersey number must be between 1 and 10 characters")
    @Schema(description = "Jersey / dorsal number", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    String number,

    @Size(max = 50, message = "Jersey name must not exceed 50 characters")
    @Schema(description = "Optional name printed on the jersey", example = "JORDAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String jerseyName
) {}
```

### 4.2 UpdatePlayerRequest
```java
package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for updating an existing player's details")
public record UpdatePlayerRequest(
    @NotNull(message = "Team ID is required")
    @Positive(message = "Team ID must be a positive number")
    @Schema(description = "ID of the associated team", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long teamId,

    @NotBlank(message = "First name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Schema(description = "Player's first name", example = "Michael", requiredMode = Schema.RequiredMode.REQUIRED)
    String firstName,

    @NotBlank(message = "Last name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Schema(description = "Player's last name", example = "Jordan", requiredMode = Schema.RequiredMode.REQUIRED)
    String lastName,

    @NotBlank(message = "Jersey number is required and cannot be blank")
    @Size(min = 1, max = 10, message = "Jersey number must be between 1 and 10 characters")
    @Schema(description = "Jersey / dorsal number", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    String number,

    @Size(max = 50, message = "Jersey name must not exceed 50 characters")
    @Schema(description = "Optional name printed on the jersey", example = "JORDAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String jerseyName
) {}
```

### 4.3 PlayerResponse
```java
package com.basketapp.bcmserver.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Representation of a player record")
public record PlayerResponse(
    @Schema(description = "Unique numeric identifier of the player", example = "1")
    Long id,

    @Schema(description = "Unique numeric identifier of the associated team", example = "1")
    Long teamId,

    @Schema(description = "Player's first name", example = "Michael")
    String firstName,

    @Schema(description = "Player's last name", example = "Jordan")
    String lastName,

    @Schema(description = "Player's jersey/dorsal number", example = "23")
    String number,

    @Schema(description = "Player's jersey display name", example = "JORDAN")
    String jerseyName,

    @Schema(description = "UTC timestamp when the record was created", example = "2026-10-31T12:00:00Z")
    Instant createdAt
) {}
```

---

## 5. Persistence JPA Entity (`infrastructure.adapter.out.persistence.entity`)

```java
package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "players")
public class PlayerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "jersey_name")
    private String jerseyName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PlayerJpaEntity() {}

    public PlayerJpaEntity(Long id, Long teamId, String firstName, String lastName, String number, String jerseyName, Instant createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.number = number;
        this.jerseyName = jerseyName;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getJerseyName() { return jerseyName; }
    public void setJerseyName(String jerseyName) { this.jerseyName = jerseyName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

---

## 6. MapStruct Layer Mappings

### 6.1 Web Mapper (`PlayerWebMapper`)
- `toCommand(CreatePlayerRequest request)` → `CreatePlayerCommand`
- `toCommand(Long id, UpdatePlayerRequest request)` → `UpdatePlayerCommand`
- `toResponse(Player player)` → `PlayerResponse`
- `toResponseList(List<Player> players)` → `List<PlayerResponse>`

### 6.2 Persistence Mapper (`PlayerPersistenceMapper`)
- `toDomain(PlayerJpaEntity entity)` → `Player`
- `toJpaEntity(Player domain)` → `PlayerJpaEntity`
- `toDomainList(List<PlayerJpaEntity> entities)` → `List<Player>`

