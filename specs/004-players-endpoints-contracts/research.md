# Research & Technical Decisions: Players Contracts and Endpoints

**Feature Branch**: `004-players-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-31  

This document details the architectural and implementation decisions for the protected CRUD endpoints of the `players` resource in `bcmServer`.

---

## 1. Authentication Precedence & Security Pipeline

### Context
The user requirement and functional requirements (FR-002, FR-003, FR-004, SC-001) mandate that all player routes are private, requiring a valid JWT token, and that JWT token verification **MUST** strictly precede payload inspection or database queries. If a request is unauthenticated or has an invalid/expired token, the server must return `401 Unauthorized` without evaluating invalid payloads or touching the persistence layer.

### Decision
Utilize the established Spring Security filter chain with `JwtAuthenticationFilter` placed before `UsernamePasswordAuthenticationFilter`. The route pattern `/players/**` and `/api/v1/players/**` will be covered under `anyRequest().authenticated()` in `SecurityConfig`. Any authentication failure triggers `AuthenticationEntryPoint`, returning an RFC 7807 `ProblemDetail` with status `401 Unauthorized` and `text/plain` or `application/problem+json` response before Spring MVC's `DispatcherServlet` triggers controller argument resolution or `@Valid` evaluation.

### Rationale
- Spring Security's servlet filter chain executes at the servlet container boundary before Spring MVC controllers, argument resolvers, and validation interceptors run.
- Guarantees 100% token validation precedence: unauthenticated requests with malformed JSON or invalid schema values will never reach validation handlers or produce a `400 Bad Request`.
- Avoids redundant authentication checks in controller or use-case code, keeping domain and application layers clean.

### Alternatives Considered
- *Custom Controller Interceptor*: Running authentication checks inside a Spring `HandlerInterceptor`. Rejected because filter chains execute earlier in the pipeline and provide standard security context setup for the thread.
- *Manual token check in controller*: Evaluating `Authorization` header inside `@PostMapping`. Rejected because it introduces code duplication, bypasses Spring Security, and violates the single responsibility principle.

---

## 2. Pre-Database Boundary Validation & Error Reporting

### Context
FR-005, FR-006, and SC-002 require all incoming request payloads (`POST /players`, `PUT /players/{id}`) and path/query parameters (`id`, `teamId`) to be validated before invocation of application use cases or persistence logic. Validation failures must return HTTP `400 Bad Request` with structured error details (RFC 7807) and perform zero database queries.

### Decision
- Use standard `jakarta.validation` annotations (`@NotNull`, `@Positive`, `@NotBlank`, `@Size`) on request DTO records (`CreatePlayerRequest`, `UpdatePlayerRequest`).
- Annotate `PlayerController` with `@Validated` and use `@Valid @RequestBody` on payload parameters, `@Positive` on `@PathVariable("id") Long id`, and `@Positive` on optional `@RequestParam(required = false) Long teamId`.
- Translate validation exceptions (`MethodArgumentNotValidException`, `ConstraintViolationException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`) in `GlobalExceptionHandler` to standard RFC 7807 `ProblemDetail` responses with an `invalidParams` map.
- Strip or trim string fields to prevent whitespace-only bypasses, enforcing non-blank constraints.

### Rationale
- Declarative validation at the web adapter layer catches all structural and constraint errors immediately at the boundary.
- Zero persistence overhead: Spring MVC halts request handling upon validation failure before calling application services.
- Consistent client experience with clear RFC 7807 problem details highlighting exactly which fields failed validation.

### Alternatives Considered
- *Manual validation inside domain entities*: While domain entities also validate invariants, relying solely on them causes validation failures to trigger deeper in execution, risking unhandled exceptions or inconsistent error payloads.
- *Custom validator classes*: Implementing `org.springframework.validation.Validator`. Rejected as `jakarta.validation` annotations provide cleaner, standardized, and declarative schema definitions compatible with OpenAPI generation.

---

## 3. Team Existence Verification & Referential Integrity

### Context
A player must belong to an existing team (`team_id` foreign key referencing `teams(id)` with `ON DELETE CASCADE`). If a request specifies a `team_id` that does not exist in the database, the system must return `404 Not Found` (or a clear error) rather than failing with an unhandled database constraint exception (`DataIntegrityViolationException`).

### Decision
- Application use cases (`CreatePlayerUseCase`, `UpdatePlayerUseCase`) will inject and query `TeamRepositoryPort.existsById(teamId)` (or `TeamRepositoryPort.findById(teamId)`).
- If `teamRepositoryPort.existsById(teamId)` returns `false`, throw `TeamNotFoundException("Team not found with id: " + teamId)`.
- `GlobalExceptionHandler` already translates `TeamNotFoundException` into an RFC 7807 `404 Not Found` response.
- On deletion of a team, PostgreSQL's `ON DELETE CASCADE` constraint on `players.team_id` ensures referential integrity without manual cascading logic in code.
- When deleting a player (`DELETE /players/{id}`), PostgreSQL's `ON DELETE CASCADE` defined on `player_stats.player_id` ensures referential integrity for dependent statistics records.

### Rationale
- Clean hexagonal separation: the application service orchestrates the business check using ports.
- Avoids relying on database foreign key exceptions as primary business logic, providing precise, deterministic error responses.
- `TeamRepositoryPort` already provides `existsById(Long id)` from the teams feature.

### Alternatives Considered
- *Letting the database throw foreign key violation*: Catching `DataIntegrityViolationException` in `GlobalExceptionHandler`. Rejected because distinguishing between foreign key violations on `team_id` vs other constraint violations is fragile and couples HTTP status codes to SQL error codes.
- *Calling a remote service*: Over-engineered since teams and players reside in the same modular monolith and database.

---

## 4. Database Schema, Data Types & Flyway Migration

### Context
The PostgreSQL schema contract in `docs/data_models.md` defines `players` as:
- `id`: `SERIAL` (or `BIGSERIAL`) PRIMARY KEY
- `team_id`: `INTEGER` (or `BIGINT`) NOT NULL, FK `teams(id)`, `ON DELETE CASCADE`
- `first_name`: `TEXT` NOT NULL
- `last_name`: `TEXT` NOT NULL
- `number`: `TEXT` NOT NULL
- `jersey_name`: `TEXT` NULLABLE
- `created_at`: `TIMESTAMPTZ` NOT NULL, DEFAULT CURRENT_TIMESTAMP

### Decision
- Create Flyway migration script `src/main/resources/db/migration/V3__create_players_table.sql`.
- Use `BIGSERIAL PRIMARY KEY` for `id` and `BIGINT` for `team_id` to align with Java `Long` and the previous `teams` migration (`V2__create_teams_table.sql`).
- Define `number` as `TEXT NOT NULL` to support values like `"00"`, `"0"`, `"7"`, `"23"`, `"99"`.
- Define `jersey_name` as `TEXT NULL` to accommodate optional jersey display names.
- Create index `idx_players_team_id ON players (team_id)` to optimize roster retrieval and `GET /players?teamId={teamId}` queries.
- Hibernate `ddl-auto` remains configured as `validate` or `none` per the bcmServer constitution.

### Rationale
- Exact alignment with `docs/data_models.md` and existing migrations (`V1`, `V2`).
- 64-bit identifiers (`BIGINT`/`BIGSERIAL`) provide future-proofing and match Java `Long` representations in domain models.
- Database index on `team_id` prevents full-table scans when filtering players by team.

### Alternatives Considered
- *Integer representation for jersey numbers*: Rejected because basketball jersey numbers can legitimately be `"00"` (distinct from `"0"`) and may include non-pure numeric symbols in certain leagues.
- *Omitting index on team_id*: Rejected because roster queries by team are frequent and critical for game management.

---

## 5. Hexagonal Architecture & Domain Purity

### Context
Principle I of the bcmServer constitution requires strict Ports and Adapters separation:
- Domain core must be pure Java 21 with zero Spring, JPA, or web annotations.
- Model isolation: DTOs, domain models, and JPA `@Entity` classes must remain separate.
- Conversions performed via MapStruct compile-time mappers.

### Decision
1. **Domain Model (`com.basketapp.bcmserver.domain.model`)**:
   - `Player.java`: Pure Java 21 class/record encapsulating player attributes (`id`, `teamId`, `firstName`, `lastName`, `number`, `jerseyName`, `createdAt`) and domain invariant validations.
2. **Domain Exception (`com.basketapp.bcmserver.domain.exception`)**:
   - `PlayerNotFoundException.java`: Runtime exception thrown when a requested player ID is not found.
3. **Application Inbound Ports (`com.basketapp.bcmserver.application.port.in`)**:
   - `CreatePlayerUseCase`, `GetPlayerUseCase`, `UpdatePlayerUseCase`, `DeletePlayerUseCase`.
   - Command objects: `CreatePlayerCommand`, `UpdatePlayerCommand`.
4. **Application Outbound Port (`com.basketapp.bcmserver.application.port.out`)**:
   - `PlayerRepositoryPort`: Defining `save`, `findById`, `findAll`, `findByTeamId`, `deleteById`, `existsById`.
5. **Application Service (`com.basketapp.bcmserver.application.service`)**:
   - `PlayerService`: Implements all inbound use cases, coordinating with `PlayerRepositoryPort` and `TeamRepositoryPort`.
6. **Infrastructure Web Adapter (`com.basketapp.bcmserver.infrastructure.adapter.in.web`)**:
   - `PlayerController`: Maps `/api/v1/players` and `/players`.
   - DTOs: `CreatePlayerRequest`, `UpdatePlayerRequest`, `PlayerResponse`.
   - Mapper: `PlayerWebMapper` (MapStruct).
7. **Infrastructure Persistence Adapter (`com.basketapp.bcmserver.infrastructure.adapter.out.persistence`)**:
   - `PlayerJpaEntity`: `@Entity` with `@Table(name = "players")`.
   - `SpringDataPlayerRepository`: Spring Data JPA repository interface.
   - `PlayerRepositoryAdapter`: Implements `PlayerRepositoryPort`.
   - Mapper: `PlayerPersistenceMapper` (MapStruct).

### Rationale
- Completely adheres to Hexagonal Architecture and ensures testability without starting the Spring context.
- Maintains strict boundary decoupling; modifications to persistence or web protocols have zero impact on business rules.
- MapStruct generates high-performance, type-safe mapping code at build time.

### Alternatives Considered
- *Using Spring Data JPA entity directly in controller*: Rejected as it violates Hexagonal Architecture and Constitution Principle I.
- *Single generic CRUD repository*: Rejected in favor of explicit domain repository ports tailored to player requirements.

---

## 6. REST API Design & Routing Conventions

### Context
FR-001 specifies RESTful endpoints:
- `POST /players`: Create player
- `GET /players`: Retrieve list of players (optional `teamId` filter)
- `GET /players/{id}`: Retrieve single player
- `PUT /players/{id}`: Update player details
- `DELETE /players/{id}`: Delete player

### Decision
- Annotate `PlayerController` with `@RequestMapping({"/api/v1/players", "/players"})` matching `TeamController` convention.
- HTTP Verbs and Status Codes:
  - `POST` → `201 Created` with `Location: /api/v1/players/{id}` header and player JSON body.
  - `GET` → `200 OK` with JSON array (filtered by `teamId` query parameter if provided).
  - `GET /{id}` → `200 OK` with player JSON body or `404 Not Found`.
  - `PUT /{id}` → `200 OK` with updated player JSON body or `404 Not Found`.
  - `DELETE /{id}` → `204 No Content` with empty body or `404 Not Found`.
  - Invalid validation payload/param → `400 Bad Request` (RFC 7807 `ProblemDetail`).
  - Missing/invalid JWT → `401 Unauthorized` (RFC 7807 `ProblemDetail`).
- Provide standalone OpenAPI specification in `specs/004-players-endpoints-contracts/contracts/players-api.yaml`.

### Rationale
- Aligns with REST standards and existing patterns in `TeamController` and `AuthController`.
- Dual path support ensures backward and forwards compatibility across client apps.
- `Location` header conforms to RFC 7231 for created resources.

### Alternatives Considered
- *Only `/players` or only `/api/v1/players`*: Supporting both paths through `@RequestMapping({"/api/v1/players", "/players"})` guarantees consistency with `TeamController` and satisfies client configurations without code divergence.
- *Returning 200 OK on deletion*: Standard REST practice prefers `204 No Content` when no entity body is returned upon successful resource deletion.

