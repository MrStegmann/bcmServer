# Research & Technical Decisions: Games Contracts and Endpoints

**Feature Branch**: `005-games-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-31  

This document details the architectural and implementation decisions for the protected CRUD endpoints of the `games` resource in `bcmServer`.

---

## 1. Authentication Precedence & Security Pipeline

### Context
User requirements and functional requirements (FR-002, FR-003, FR-004, SC-001) mandate that all game routes are private, requiring a valid JWT token, and that JWT token verification **MUST** strictly precede payload inspection or database queries. If a request is unauthenticated, missing the token, or provides an invalid/expired token, the server must return `401 Unauthorized` without evaluating invalid payloads or touching the persistence layer.

### Decision
Utilize the established Spring Security filter chain with `JwtAuthenticationFilter` placed before `UsernamePasswordAuthenticationFilter`. The route patterns `/games/**`, `/api/games/**`, and `/api/v1/games/**` are protected by `anyRequest().authenticated()` in `SecurityConfig`. Any authentication failure halts the request at the servlet container boundary before Spring MVC controllers, argument resolvers, and validation interceptors run, returning an RFC 7807 `ProblemDetail` with status `401 Unauthorized`.

### Rationale
- Spring Security filter chains execute prior to Spring MVC's `DispatcherServlet` routing and validation layers.
- Guarantees 100% token validation precedence: unauthenticated requests containing malformed JSON or invalid schema values will never trigger validation handlers or produce a `400 Bad Request`.
- Keeps domain and application use-case layers completely decoupled from security and authentication infrastructure.

### Alternatives Considered
- *Custom Controller Interceptor*: Intercepting requests inside Spring `HandlerInterceptor`. Rejected because servlet filters execute earlier and standard Spring Security integration is cleaner and centrally configured.
- *Manual Token Checks in Controller*: Parsing the `Authorization` header within controller methods. Rejected because it violates separation of concerns, duplicates logic, and risks security leaks.

---

## 2. Pre-Database Boundary Validation & Error Reporting

### Context
FR-005, FR-006, and SC-002 require all incoming request payloads (`POST /api/games`, `PUT /api/games/{id}`) and path/query parameters (`id`, `teamId`) to be validated before invocation of application use cases or persistence logic. Validation failures must return HTTP `400 Bad Request` with structured error details (RFC 7807) and perform zero database queries.

### Decision
- Use declarative `jakarta.validation` annotations (`@NotNull`, `@Positive`, `@NotBlank`, `@Min(0)`, `@Pattern`) on request DTO records (`CreateGameRequest`, `UpdateGameRequest`).
- Restrict `status` to allowed enum values (`pending`, `live`, `finished`) using custom or regex validation (`^(pending|live|finished)$`).
- Annotate `GameController` with `@Validated` and use `@Valid @RequestBody` on payload parameters, `@Positive` on `@PathVariable("id") Long id`, and `@Positive` on optional `@RequestParam(required = false) Long teamId`.
- Translate validation exceptions (`MethodArgumentNotValidException`, `ConstraintViolationException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`) in `GlobalExceptionHandler` to standard RFC 7807 `ProblemDetail` responses with an `invalidParams` map.
- Strip or trim string fields (`opponent`, `round`, `date`) to prevent whitespace-only bypasses, enforcing non-blank constraints.

### Rationale
- Declarative validation at the web adapter layer catches all structural and constraint errors immediately at the boundary.
- Zero persistence overhead: Spring MVC halts request handling upon validation failure before calling application services or database repositories.
- Consistent client experience with clear RFC 7807 problem details highlighting exactly which fields failed validation.

### Alternatives Considered
- *Validation inside JPA entities*: JPA entity annotations execute during transaction flushing or persistence operations, too late in the lifecycle, causing database round-trips and unhandled persistence exceptions.
- *Manual validation code in service layer*: Clutters business logic with presentation/schema validation concerns that belong at the web adapter boundary.

---

## 3. Team Existence Verification & Referential Integrity

### Context
A game must be associated with an existing home team (`team_id` foreign key referencing `teams(id)` with `ON DELETE CASCADE`). If a request specifies a `team_id` that does not exist in the database, the system must return `404 Not Found` (or an informative error) rather than failing with an unhandled database constraint exception (`DataIntegrityViolationException`).

### Decision
- Application use cases (`CreateGameUseCase`, `UpdateGameUseCase`) will inject and query `TeamRepositoryPort.existsById(teamId)`.
- If `teamRepositoryPort.existsById(teamId)` returns `false`, throw `TeamNotFoundException("Team not found with id: " + teamId)`.
- `GlobalExceptionHandler` already translates `TeamNotFoundException` into an RFC 7807 `404 Not Found` response.
- On deletion of a team, PostgreSQL's `ON DELETE CASCADE` constraint on `games.team_id` ensures referential integrity without manual cascading logic in code.
- When deleting a game (`DELETE /api/games/{id}`), PostgreSQL's `ON DELETE CASCADE` defined on `player_stats.game_id` and `match_cache.match_id` per `docs/data_models.md` guarantees automated referential cleanup.

### Rationale
- Clean hexagonal separation: the application service orchestrates the business check using existing outbound ports.
- Avoids relying on database foreign key exceptions as primary business logic, providing precise, deterministic error responses.
- `TeamRepositoryPort` already provides `existsById(Long id)` from the teams feature.

### Alternatives Considered
- *Catching `DataIntegrityViolationException`*: Catching SQL constraint violations in `GlobalExceptionHandler`. Rejected because distinguishing between foreign key violations on `team_id` vs other constraint violations is fragile and couples HTTP status codes to SQL error codes.

---

## 4. Game Domain Model & Score/Foul Encapsulation

### Context
Per `docs/data_models.md`, a game has basic match metadata (`team_id`, `opponent`, `round`, `date`, `status`, `created_at`) as well as 10 score fields (local quarters `result_c1`..`result_c4`, local overtime `result_extra`, rival quarters `result_c1_opponent`..`result_c4_opponent`, rival overtime `result_extra_opponent`) and 10 foul fields (local quarters `fouls_c1`..`fouls_c4`, local overtime `fouls_extra`, rival quarters `fouls_c1_opponent`..`fouls_c4_opponent`, rival overtime `fouls_extra_opponent`). All score and foul fields default to 0 and must be non-negative.

### Decision
- Implement `Game` as a pure Java 21 domain entity in `domain.model.Game`.
- Encapsulate scores and fouls directly as integer properties (or value objects if desirable, but keeping flat fields matching data model for direct MapStruct mapping simplicity) with invariant validation in the domain entity:
  - All score and foul values must be non-negative (`>= 0`).
  - Opponent, round, and date must be non-blank.
  - Status must be a valid `GameStatus` enum (`PENDING`, `LIVE`, `FINISHED`) with string representation (`pending`, `live`, `finished`).
- In `CreateGameRequest`, optional score and foul fields default to `0`, `round` defaults to `"1"`, and `status` defaults to `"pending"`.
- In `UpdateGameRequest`, all quarter scores and fouls can be updated together with match status and details.

### Rationale
- Retains 100% adherence to pure Java 21 domain modeling with zero framework or persistence annotations.
- Domain invariants guarantee that even if instantiated outside web validation, a `Game` entity cannot have negative scores, negative fouls, or invalid status.
- Matches exact field naming and constraints defined in `docs/data_models.md`.

### Alternatives Considered
- *Nested Score and Foul Value Objects*: Grouping quarters into `QuarterScores` / `QuarterFouls` objects. While clean for domain modeling, mapping between nested objects and flat PostgreSQL columns adds overhead and potential mismatch with `docs/data_models.md`. Flat domain fields or clean accessors provide direct 1:1 mapping with MapStruct.

---

## 5. URL Path Aliasing & REST Compatibility

### Context
FR-001 and Acceptance Scenarios specify routes under `/api/games` as well as `/games`. Existing codebase conventions in `TeamController` and `PlayerController` use `@RequestMapping({"/api/v1/...", "/..."})`.

### Decision
Annotate `GameController` with `@RequestMapping({"/api/v1/games", "/api/games", "/games"})`. This ensures full backward and forward compatibility for clients invoking `/api/games`, `/games`, or versioned `/api/v1/games`.

### Rationale
- Solves routing for all client conventions without duplicating controller classes or handlers.
- Both `/api/games` and `/games` are explicitly mentioned in spec and acceptance scenarios.

### Alternatives Considered
- *Supporting only `/api/games`*: Could break clients or tests expecting `/games` or `/api/v1/games`. Supporting all three paths is standard in Spring MVC via multiple path strings.

---

## 6. Schema Migration & Flyway Strategy

### Context
PostgreSQL is the system of record. Relational schema evolution is governed strictly via Flyway migrations per Constitution Principle III.
Existing migrations:
- `V1__create_users_table.sql`
- `V2__create_teams_table.sql`
- `V3__create_players_table.sql`

### Decision
Create migration `src/main/resources/db/migration/V4__create_games_table.sql` defining:
- `id BIGSERIAL PRIMARY KEY`
- `team_id BIGINT NOT NULL` with `CONSTRAINT fk_games_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE`
- `opponent TEXT NOT NULL`
- `round TEXT NOT NULL DEFAULT '1'`
- `date TEXT NOT NULL`
- `status TEXT NOT NULL DEFAULT 'pending'`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
- Scores: `result_c1` through `result_c4`, `result_extra`, `result_c1_opponent` through `result_c4_opponent`, `result_extra_opponent` (all `INTEGER NOT NULL DEFAULT 0`)
- Fouls: `fouls_c1` through `fouls_c4`, `fouls_extra`, `fouls_c1_opponent` through `fouls_c4_opponent`, `fouls_extra_opponent` (all `INTEGER NOT NULL DEFAULT 0`)
- Index on `team_id` (`idx_games_team_id`) for high-performance filtering.

### Rationale
- Completely adheres to `docs/data_models.md` and existing migrations (`V1`, `V2`, `V3`).
- Non-null defaults for scores and fouls eliminate SQL null handling complexities and guarantee database-level consistency.
- `ON DELETE CASCADE` handles team deletion cleanly.

