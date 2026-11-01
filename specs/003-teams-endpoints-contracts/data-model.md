# Data Model: Team Management

**Feature**: `003-teams-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-30  

---

## 1. Domain Entities & Value Objects

The domain model is situated in `com.basketapp.bcmserver.domain.model` as pure Java 21 classes/records with zero framework dependencies.

### 1.1 Team (Domain Entity)

Represents a basketball team within the club or competition system.

```java
package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Team {
    private final Long id;
    private final String name;
    private final String category;
    private final Instant createdAt;

    public Team(Long id, String name, String category, Instant createdAt) {
        this.id = id;
        this.name = sanitizeAndValidateName(name);
        this.category = sanitizeCategory(category);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    // Getters, equals, hashCode, toString
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
```

#### Invariants & Validation Rules
- `id`: Non-negative numeric identifier; null prior to persistence, non-null once persisted.
- `name`: Mandatory, non-empty, non-blank, max 100 characters. Automatically trimmed of leading and trailing whitespace.
- `category`: Optional/nullable. Empty or whitespace-only values are normalized to `null`. If non-empty, max 50 characters.
- `createdAt`: Mandatory, default current UTC instant (`Instant.now()`), immutable.

---

## 2. Inbound & Outbound DTOs

### 2.1 Web Inbound Request DTOs (`infrastructure.adapter.in.web.dto`)

#### CreateTeamRequest
```java
public record CreateTeamRequest(
    @NotBlank(message = "Team name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
    String name,

    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
```

#### UpdateTeamRequest
```java
public record UpdateTeamRequest(
    @NotBlank(message = "Team name is required and cannot be blank")
    @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
    String name,

    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
```

### 2.2 Web Outbound Response DTOs (`infrastructure.adapter.in.web.dto`)

#### TeamResponse
```java
public record TeamResponse(
    Long id,
    String name,
    String category,
    Instant createdAt
) {}
```

---

## 3. Database Schema & JPA Entity Mapping

### 3.1 Relational Schema Definition (`teams` table)

Derived from `docs/data_models.md` and implemented in Flyway migration `V2__create_teams_table.sql`:

```sql
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_teams_name ON teams (name);
```

### 3.2 JPA Persistence Entity (`TeamJpaEntity`)

Situates strictly inside `com.basketapp.bcmserver.infrastructure.adapter.out.persistence.entity`:

```java
@Entity
@Table(name = "teams")
public class TeamJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "category", nullable = true, columnDefinition = "TEXT")
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Default constructor, getters, setters
}
```

---

## 4. Entity Relationships & Referential Integrity

```
+--------------------+           1 : N           +--------------------+
|       teams        | ------------------------> |      players       |
| (ON DELETE CASCADE)|                           | (FK: team_id)      |
+--------------------+                           +--------------------+
          |
          | 1 : N
          v
+--------------------+
|       games        |
| (FK: team_id)      |
| (ON DELETE CASCADE)|
+--------------------+
```

- When a `team` is deleted, the database enforces `ON DELETE CASCADE` on `players` and `games` tables (as detailed in `docs/data_models.md`).
- Spring Data JPA repository issues `DELETE FROM teams WHERE id = :id`.
- Referential integrity is maintained at the database engine level.

---

## 5. Domain Exceptions

Located in `com.basketapp.bcmserver.domain.exception`:

- `TeamNotFoundException`:
  ```java
  public class TeamNotFoundException extends DomainException {
      public TeamNotFoundException(Long id) {
          super("Team with ID " + id + " was not found");
      }
  }
  ```
  Handled by `GlobalExceptionHandler` returning RFC 7807 `404 Not Found` ProblemDetail.

