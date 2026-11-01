# Research & Technical Decisions: Player Stats Contracts and Endpoints

**Feature Branch**: `006-player-stats-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-31  

This document details the architectural and technical implementation decisions for the protected CRUD endpoints of the `player_stats` resource in `bcmServer`.

---

## 1. Authentication Precedence & Security Pipeline

### Context
User specifications and functional requirements (FR-002, FR-003, FR-004, SC-001) dictate that all player statistics routes are private, requiring a valid JWT Bearer token, and that JWT token verification **MUST** strictly precede payload evaluation, parameter checking, or database queries. If a request is unauthenticated, missing a token, or provides an invalid/expired token, the server must return HTTP `401 Unauthorized` without evaluating invalid payloads or accessing the database.

### Decision
Utilize the established Spring Security filter chain with `JwtAuthenticationFilter` positioned before `UsernamePasswordAuthenticationFilter`. The route patterns `/player-stats/**`, `/api/player-stats/**`, and `/api/v1/player-stats/**` are protected by `.anyRequest().authenticated()` in `SecurityConfig`. Any authentication failure is trapped at the servlet container filter level prior to Spring MVC's `DispatcherServlet` routing, parameter resolving, or payload validation, returning an RFC 7807 `ProblemDetail` with status `401 Unauthorized`.

### Rationale
- Servlet filters execute prior to Spring MVC controllers, argument resolvers, and validation interceptors.
- Guarantees 100% token validation precedence: unauthenticated requests containing malformed JSON or invalid schema values will never trigger validation handlers or produce a `400 Bad Request`.
- Keeps domain and application use-case layers completely decoupled from security and authentication infrastructure.

### Alternatives Considered
- *Custom Controller Interceptor*: Intercepting requests inside Spring `HandlerInterceptor`. Rejected because servlet filters execute earlier and standard Spring Security integration is cleaner, stateless, and centrally configured.
- *Manual Token Checks in Controller*: Parsing the `Authorization` header within controller methods. Rejected because it violates separation of concerns, duplicates logic, and risks security leaks.

---

## 2. Pre-Database Boundary Validation & Error Reporting

### Context
FR-005, FR-006, and SC-002 require all incoming request payloads (`POST /api/player-stats`, `PUT /api/player-stats/{id}`) and path/query parameters (`id`, `gameId`, `playerId`) to be strictly validated before invocation of application use cases or persistence logic. Validation failures must return HTTP `400 Bad Request` with structured RFC 7807 problem details and perform zero database queries.

### Decision
- Use declarative `jakarta.validation` annotations on request DTO records (`CreatePlayerStatsRequest`, `UpdatePlayerStatsRequest`):
  - `@NotNull` and `@Positive` on `game_id` and `player_id`.
  - `@Min(0)` on `minutes`.
  - `@Min(0)` and `@Max(59)` on `seconds` (playing time seconds).
  - `@Min(0)` on counting stats: `t1a` (free throws attempted), `t1i` (free throws made), `t2a` (2pt attempted), `t2i` (2pt made), `t3a` (3pt attempted), `t3i` (3pt made), `oreb` (offensive rebounds), `dreb` (defensive rebounds), `asis` (assists), `rec` (steals), `per` (turnovers), `falt` (personal fouls).
  - Logical consistency check: Made shots (`t1i`, `t2i`, `t3i`) cannot exceed attempted shots (`t1a`, `t2a`, `t3a`). While basic non-negative constraints are enforced via `@Min(0)`, a cross-field validation or domain invariant verifies made <= attempted, returning HTTP `400 Bad Request` if violated.
  - `valuation`: Performance Index Rating (PIR) is an integer that can be negative, zero, or positive; it accepts any signed integer and defaults to `0` if null in creation requests.
- Annotate `PlayerStatsController` with `@Validated` and use:
  - `@Valid @RequestBody` on payload parameters.
  - `@Positive` on `@PathVariable("id") Long id`.
  - `@Positive` on optional `@RequestParam(required = false) Long gameId` and `@RequestParam(required = false) Long playerId`.
- Translate validation exceptions (`MethodArgumentNotValidException`, `ConstraintViolationException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`) in `GlobalExceptionHandler` to standard RFC 7807 `ProblemDetail` responses with an `invalidParams` map.

### Rationale
- Declarative validation at the web adapter layer catches all structural and constraint errors immediately at the boundary.
- Zero persistence overhead: Spring MVC halts request handling upon validation failure before calling application services or database repositories.
- Consistent client experience with clear RFC 7807 problem details highlighting exactly which fields failed validation.

### Alternatives Considered
- *Validation inside JPA entities*: JPA entity annotations execute during transaction flushing or persistence operations, too late in the lifecycle, causing database round-trips and unhandled persistence exceptions.
- *Manual validation code in service layer*: Clutters business logic with presentation/schema validation concerns that belong at the web adapter boundary.

---

## 3. Foreign Key Referencing & Referential Integrity

### Context
A player stats record must be associated with an existing game (`game_id` foreign key referencing `games(id)`) and an existing player (`player_id` foreign key referencing `players(id)`). If a creation or update request references a `game_id` or `player_id` that does not exist in the database, the system must return `404 Not Found` rather than failing with an unhandled database constraint exception (`DataIntegrityViolationException`).

### Decision
- Application use cases (`CreatePlayerStatsUseCase`, `UpdatePlayerStatsUseCase`) will inject and query `GameRepositoryPort.existsById(gameId)` and `PlayerRepositoryPort.existsById(playerId)`.
- If `!gameRepository.existsById(gameId)`, throw `GameNotFoundException("Game not found with id: " + gameId)`.
- If `!playerRepository.existsById(playerId)`, throw `PlayerNotFoundException("Player not found with id: " + playerId)`.
- Both `GameNotFoundException` and `PlayerNotFoundException` are already registered in `GlobalExceptionHandler` and return RFC 7807 `404 Not Found`.
- Similarly, looking up or modifying a non-existent player stats record throws `PlayerStatsNotFoundException("Player stats not found with id: " + id)`, which will be added to `GlobalExceptionHandler` returning RFC 7807 `404 Not Found`.
- On deletion of a referenced `game` or `player`, PostgreSQL's `ON DELETE CASCADE` constraint on `player_stats.game_id` and `player_stats.player_id` per `docs/data_models.md` guarantees automated referential cleanup.

### Rationale
- Clean hexagonal separation: the application service orchestrates business checks using existing outbound ports.
- Avoids relying on raw SQL foreign key exceptions as primary business logic, providing precise, deterministic error responses.
- `GameRepositoryPort` and `PlayerRepositoryPort` already provide `existsById(Long id)`.

### Alternatives Considered
- *Catching `DataIntegrityViolationException`*: Catching SQL constraint violations in `GlobalExceptionHandler`. Rejected because distinguishing between foreign key violations on `game_id` vs `player_id` vs other constraint violations is fragile and couples HTTP status codes to SQL error codes.

---

## 4. Database Persistence & Flyway Migration Strategy

### Context
Per `docs/data_models.md`, `player_stats` requires a relational table with columns for match participation time and 12 distinct statistical metrics. Existing migrations are `V1` through `V4`. The `player_stats` table must be introduced via a versioned Flyway migration script `V5__create_player_stats_table.sql`.

### Decision
Create Flyway migration `src/main/resources/db/migration/V5__create_player_stats_table.sql` with:
- `id BIGSERIAL PRIMARY KEY`
- `game_id BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE`
- `player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE`
- `minutes INTEGER NOT NULL DEFAULT 0`
- `seconds INTEGER NOT NULL DEFAULT 0`
- `t1a INTEGER NOT NULL DEFAULT 0`
- `t1i INTEGER NOT NULL DEFAULT 0`
- `t2a INTEGER NOT NULL DEFAULT 0`
- `t2i INTEGER NOT NULL DEFAULT 0`
- `t3a INTEGER NOT NULL DEFAULT 0`
- `t3i INTEGER NOT NULL DEFAULT 0`
- `oreb INTEGER NOT NULL DEFAULT 0`
- `dreb INTEGER NOT NULL DEFAULT 0`
- `asis INTEGER NOT NULL DEFAULT 0`
- `rec INTEGER NOT NULL DEFAULT 0`
- `per INTEGER NOT NULL DEFAULT 0`
- `falt INTEGER NOT NULL DEFAULT 0`
- `valuation INTEGER NOT NULL DEFAULT 0`
- Indexes:
  - `idx_player_stats_game_id ON player_stats (game_id)`
  - `idx_player_stats_player_id ON player_stats (player_id)`
  - `idx_player_stats_game_player ON player_stats (game_id, player_id)`

### Rationale
- Versioned Flyway migration ensures deterministic schema evolution aligned with Constitution Principle III.
- Foreign keys with `ON DELETE CASCADE` guarantee that deleting a match or a player automatically cleans up statistical history.
- Indexes on `game_id`, `player_id`, and `(game_id, player_id)` optimize filtered collection queries (`GET /api/player-stats?gameId=X`, `GET /api/player-stats?playerId=Y`, and combination).

### Alternatives Considered
- *Hibernate automatic DDL*: Prohibited by Constitution Principle III (`ddl-auto` must remain `validate` or `none`).

---

## 5. Hexagonal Architecture & Object Mapping Strategy

### Context
Constitution Principle I requires pure domain models with zero framework dependencies, application ports (use cases and repository interfaces), and isolated adapters (controllers and JPA repositories) using MapStruct for compile-time mapping.

### Decision
- **Domain Model**: `PlayerStats` entity in `domain.model` written in pure Java 21 without Spring, Jakarta, or JPA imports. Business invariants (non-negative metrics, 0-59 seconds, valid foreign keys) are enforced in domain constructors and update methods.
- **Application Ports**:
  - Inbound: `CreatePlayerStatsUseCase`, `GetPlayerStatsUseCase`, `UpdatePlayerStatsUseCase`, `DeletePlayerStatsUseCase` with immutable command records (`CreatePlayerStatsCommand`, `UpdatePlayerStatsCommand`).
  - Outbound: `PlayerStatsRepositoryPort` defining repository contract.
- **Application Service**: `PlayerStatsService` implementing inbound use cases and coordinating with outbound ports (`PlayerStatsRepositoryPort`, `GameRepositoryPort`, `PlayerRepositoryPort`).
- **Web Adapter**: `PlayerStatsController`, DTO records (`CreatePlayerStatsRequest`, `UpdatePlayerStatsRequest`, `PlayerStatsResponse`), and `PlayerStatsWebMapper` (MapStruct).
- **Persistence Adapter**: `PlayerStatsJpaEntity`, `SpringDataPlayerStatsRepository`, `PlayerStatsRepositoryAdapter`, and `PlayerStatsPersistenceMapper` (MapStruct).

### Rationale
- Completely protects domain logic from framework evolution.
- Compile-time MapStruct mapping prevents reflection overhead and guarantees type safety between DTOs, domain models, and JPA entities.
- Persistence entities never leak to the web layer.

---

## 6. Valuation (PIR) Computation & Flexibility

### Context
In basketball statistics, Performance Index Rating (PIR) is calculated as:
`(points + rebounds + assists + steals + fouls_drawn + blocks) - (missed_field_goals + missed_free_throws + turnovers + fouls_committed)`.
However, `docs/data_models.md` specifies `valuation INTEGER DEFAULT 0` and allows clients to provide the calculated valuation directly or allow the system to default/compute it.

### Decision
- Support client-provided `valuation` in `CreatePlayerStatsRequest` and `UpdatePlayerStatsRequest`.
- If `valuation` is null in creation requests, default it to `0` (or compute from available stat fields if desired, maintaining consistency with client input).
- Ensure `valuation` can accept signed values (e.g., negative PIR scores like `-3`).

### Rationale
- Fully respects the database schema contract (`valuation INTEGER DEFAULT 0`) while supporting realistic match scorekeeping software that may compute PIR client-side or provide custom valuation logic.

