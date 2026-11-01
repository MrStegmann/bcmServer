# Implementation Plan: Player Stats Contracts and Endpoints

**Branch**: `006-player-stats-endpoints-contracts` | **Date**: 2026-10-31 | **Spec**: [specs/006-player-stats-endpoints-contracts/spec.md](spec.md)

**Input**: Feature specification from `/specs/006-player-stats-endpoints-contracts/spec.md`

## Summary

Implement private, RESTful CRUD endpoints for the `player_stats` resource (`POST /api/player-stats`, `GET /api/player-stats`, `GET /api/player-stats/{id}`, `PUT /api/player-stats/{id}`, `DELETE /api/player-stats/{id}`, with path aliasing for `/player-stats` and `/api/v1/player-stats`) backed by PostgreSQL and managed by Flyway (`V5__create_player_stats_table.sql`). The implementation strictly enforces Hexagonal Architecture (Ports and Adapters), stateless JWT authentication precedence (401 Unauthorized before any validation or database query), pre-database payload/parameter validation (`jakarta.validation` producing RFC 7807 `ProblemDetail`), compile-time MapStruct mapping, foreign key existence verification for `game_id` and `player_id`, and Springdoc OpenAPI documentation.

## Technical Context

**Language/Version**: Java 21 (LTS, records, pattern matching)  
**Primary Dependencies**: Spring Boot 3.4.2 (Spring Web MVC, Spring Security 6.x, Spring Data JPA, Spring Validation), JJWT 0.12.6, MapStruct 1.6.3, Springdoc OpenAPI 2.8.4  
**Storage**: PostgreSQL (persisted via Spring Data JPA and Flyway migration `V5__create_player_stats_table.sql` matching `docs/data_models.md`)  
**Testing**: JUnit 5, Mockito, Spring Boot Test Slice (`@WebMvcTest`, `@DataJpaTest`), Spring Security Test  
**Target Platform**: Linux/Containerized JVM (Docker Compose local runtime)  
**Project Type**: Web Service (Hexagonal RESTful API)  
**Performance Goals**: <100ms average server processing latency for CRUD operations under standard operational conditions  
**Constraints**: 
- Strict token validation precedence (401 Unauthorized before request payload evaluation or DB query)
- 100% pre-database validation on all inbound payloads, query parameters, and path parameters (RFC 7807 400 Bad Request)
- Zero framework or persistence annotation leakage into the domain core
- Pure Java 21 domain entities without Spring or JPA dependencies
- Referential integrity: verify `GameRepositoryPort.existsById(gameId)` and `PlayerRepositoryPort.existsById(playerId)` before creating or updating records, throwing 404 Not Found if missing
- Non-negative constraints for playing time (`minutes >= 0`, `0 <= seconds <= 59`) and counting stats (`t1a`, `t1i`, `t2a`, `t2i`, `t3a`, `t3i`, `oreb`, `dreb`, `asis`, `rec`, `per`, `falt` >= 0)
- Valuation (PIR) accepts signed integers (can be negative, 0, or positive) defaulting to 0
- Made shots cannot exceed attempted shots (`t1i <= t1a`, `t2i <= t2a`, `t3i <= t3a`)
- All cross-layer mapping performed via MapStruct compile-time mappers
- Full route aliasing supporting `/api/player-stats`, `/player-stats`, and `/api/v1/player-stats`
**Scale/Scope**: Granular individual game statistics tracking and box-score calculation across matches and players

## Constitution Check

*GATE: Evaluated before Phase 0 and re-evaluated post Phase 1 design.*

| Principle | Gate Status | Compliance Details |
| :--- | :---: | :--- |
| **I. Hexagonal Architecture (Ports and Adapters)** | **PASS** | `PlayerStats` domain entity is pure Java 21 in `domain.model`. Inbound ports (`CreatePlayerStatsUseCase`, `GetPlayerStatsUseCase`, `UpdatePlayerStatsUseCase`, `DeletePlayerStatsUseCase`) and outbound port (`PlayerStatsRepositoryPort`) isolate business logic. Adapters (`PlayerStatsController`, `PlayerStatsRepositoryAdapter`) encapsulate Web and JPA respectively. Cross-layer mapping is handled via MapStruct (`PlayerStatsWebMapper`, `PlayerStatsPersistenceMapper`). Persistence entities never leak to the web layer. |
| **II. API Design & Documentation-First** | **PASS** | RESTful HTTP verbs and status codes (201, 200, 204, 400, 401, 404). Documented with Springdoc OpenAPI annotations and standalone OpenAPI spec contract in `contracts/player-stats-api.yaml`. Boundary validation using `jakarta.validation` producing RFC 7807 `ProblemDetail`. |
| **III. Data Persistence & Flyway** | **PASS** | Schema evolution managed strictly via Flyway migration `V5__create_player_stats_table.sql` matching `docs/data_models.md`. Hibernate `ddl-auto` remains `none` or `validate`. JPA entity isolated in persistence adapter. Foreign key referential integrity on `game_id` and `player_id` with `ON DELETE CASCADE`. Indexes created for high performance querying. |
| **IV. Stateless Security & JWT** | **PASS** | Endpoints are protected by stateless JWT Bearer token authentication in `SecurityConfig`. Filter chain enforces token validation before controller/payload validation. Zero HTTP session state. |
| **V. Automated Testing & Quality** | **PASS** | Comprehensive testing planned across architectural rings: unit tests for domain `PlayerStats` and application `PlayerStatsService` (pure JUnit 5/Mockito), slice tests for `PlayerStatsController` (`@WebMvcTest`), and repository integration tests (`@DataJpaTest`). |

## Project Structure

### Documentation (this feature)

```text
specs/006-player-stats-endpoints-contracts/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 research decisions and rationale
├── data-model.md        # Phase 1 domain, DTO, and database models
├── contracts/           # Phase 1 API contract definitions
│   └── player-stats-api.yaml # OpenAPI 3.0 specification for Player Stats endpoints
└── quickstart.md        # Phase 1 runnable end-to-end verification guide
```

### Source Code Layout (Repository Root)

```text
src/
├── main/
│   ├── java/com/basketapp/bcmserver/
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── in/
│   │   │   │   │   ├── CreatePlayerStatsCommand.java
│   │   │   │   │   ├── CreatePlayerStatsUseCase.java
│   │   │   │   │   ├── DeletePlayerStatsUseCase.java
│   │   │   │   │   ├── GetPlayerStatsUseCase.java
│   │   │   │   │   ├── UpdatePlayerStatsCommand.java
│   │   │   │   │   └── UpdatePlayerStatsUseCase.java
│   │   │   │   └── out/
│   │   │   │       └── PlayerStatsRepositoryPort.java
│   │   │   └── service/
│   │   │       └── PlayerStatsService.java
│   │   ├── domain/
│   │   │   ├── exception/
│   │   │   │   └── PlayerStatsNotFoundException.java
│   │   │   └── model/
│   │   │       └── PlayerStats.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/web/
│   │       │   │   ├── PlayerStatsController.java
│   │       │   │   ├── dto/
│   │       │   │   │   ├── CreatePlayerStatsRequest.java
│   │       │   │   │   ├── PlayerStatsResponse.java
│   │       │   │   │   └── UpdatePlayerStatsRequest.java
│   │       │   │   ├── exception/
│   │       │   │   │   └── GlobalExceptionHandler.java (updated for PlayerStatsNotFoundException)
│   │       │   │   └── mapper/
│   │       │   │       └── PlayerStatsWebMapper.java
│   │       │   └── out/persistence/
│   │       │       ├── adapter/
│   │       │       │   └── PlayerStatsRepositoryAdapter.java
│   │       │       ├── entity/
│   │       │       │   └── PlayerStatsJpaEntity.java
│   │       │       ├── mapper/
│   │       │       │   └── PlayerStatsPersistenceMapper.java
│   │       │       └── repository/
│   │       │           └── SpringDataPlayerStatsRepository.java
│   └── resources/
│       └── db/migration/
│           └── V5__create_player_stats_table.sql
└── test/
    └── java/com/basketapp/bcmserver/
        ├── application/service/
        │   └── PlayerStatsServiceTest.java
        ├── domain/model/
        │   └── PlayerStatsTest.java
        └── infrastructure/adapter/
            ├── in/web/
            │   └── PlayerStatsControllerTest.java
            └── out/persistence/
                └── PlayerStatsRepositoryAdapterTest.java
```

**Structure Decision**: Hexagonal Architecture with complete layer decoupling matching the existing application architecture established in features 001-005.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *None* | N/A | Fully adheres to standard constitution and architecture |
