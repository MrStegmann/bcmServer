# Implementation Plan: Games Contracts and Endpoints

**Branch**: `005-games-endpoints-contracts` | **Date**: 2026-10-31 | **Spec**: [specs/005-games-endpoints-contracts/spec.md](spec.md)

**Input**: Feature specification from `/specs/005-games-endpoints-contracts/spec.md`

## Summary

Implement private, RESTful CRUD endpoints for the `games` resource (`POST /api/games`, `GET /api/games`, `GET /api/games/{id}`, `PUT /api/games/{id}`, `DELETE /api/games/{id}`, with path aliasing for `/games` and `/api/v1/games`) backed by PostgreSQL and managed by Flyway (`V4__create_games_table.sql`). The implementation strictly enforces Hexagonal Architecture (Ports and Adapters), stateless JWT authentication precedence (401 Unauthorized before any validation or database query), pre-database payload/parameter validation (`jakarta.validation` producing RFC 7807 `ProblemDetail`), compile-time MapStruct mapping, and Springdoc OpenAPI documentation.

## Technical Context

**Language/Version**: Java 21 (LTS, records, sealed interfaces, pattern matching)  
**Primary Dependencies**: Spring Boot 3.4.2 (Spring Web MVC, Spring Security 6.x, Spring Data JPA, Spring Validation), JJWT 0.12.6, MapStruct 1.6.3, Springdoc OpenAPI 2.8.4  
**Storage**: PostgreSQL (persisted via Spring Data JPA and Flyway migration `V4__create_games_table.sql` matching `docs/data_models.md`)  
**Testing**: JUnit 5, Mockito, Spring Boot Test Slice (`@WebMvcTest`, `@DataJpaTest`), Spring Security Test  
**Target Platform**: Linux/Containerized JVM (Docker Compose local runtime)  
**Project Type**: Web Service (Hexagonal RESTful API)  
**Performance Goals**: <100ms average server processing latency for CRUD operations under standard load  
**Constraints**: 
- Strict token validation precedence (401 Unauthorized before request payload evaluation or DB query)
- 100% pre-database validation on all inbound payloads, query parameters, and path parameters (RFC 7807 400 Bad Request)
- Zero framework or persistence annotation leakage into the domain core
- Pure Java 21 domain entities without Spring or JPA dependencies
- Team existence verification (`TeamRepositoryPort.existsById`) prior to game creation or update
- Game status restricted strictly to `pending`, `live`, `finished`
- Scores and fouls non-negative (>= 0), defaulting to 0 on creation if omitted
- All cross-layer mapping performed via MapStruct compile-time mappers
- Full route aliasing supporting `/api/games`, `/games`, and `/api/v1/games`
**Scale/Scope**: Match scheduling, quarter-by-quarter scorekeeping, and foul tracking for basketball tournaments and teams

## Constitution Check

*GATE: Evaluated before Phase 0 and re-evaluated post Phase 1 design.*

| Principle | Gate Status | Compliance Details |
| :--- | :---: | :--- |
| **I. Hexagonal Architecture (Ports and Adapters)** | **PASS** | `Game` and `GameStatus` domain entities are pure Java 21 in `domain.model`. Inbound ports (`CreateGameUseCase`, `GetGameUseCase`, `UpdateGameUseCase`, `DeleteGameUseCase`) and outbound port (`GameRepositoryPort`) isolate business logic. Adapters (`GameController`, `GameRepositoryAdapter`) encapsulate Web and JPA respectively. Cross-layer mapping is handled via MapStruct (`GameWebMapper`, `GamePersistenceMapper`). Persistence entities never leak to the web layer. |
| **II. API Design & Documentation-First** | **PASS** | RESTful HTTP verbs and status codes (201, 200, 204, 400, 401, 404). Documented with Springdoc OpenAPI annotations and standalone OpenAPI spec contract in `contracts/games-api.yaml`. Boundary validation using `jakarta.validation` producing RFC 7807 `ProblemDetail`. |
| **III. Data Persistence & Flyway** | **PASS** | Schema evolution managed strictly via Flyway migration `V4__create_games_table.sql` matching `docs/data_models.md`. Hibernate `ddl-auto` remains `none` or `validate`. JPA entity isolated in persistence adapter. Foreign key referential integrity on `team_id` with `ON DELETE CASCADE`. Cascading deletion to dependent `player_stats` and `match_cache`. |
| **IV. Stateless Security & JWT** | **PASS** | Endpoints are protected by stateless JWT Bearer token authentication in `SecurityConfig`. Filter chain enforces token validation before controller/payload validation. No session state. |
| **V. Automated Testing & Quality** | **PASS** | Comprehensive testing planned across architectural layers: unit tests for domain `Game` and application `GameService` (pure JUnit 5/Mockito), slice tests for `GameController` (`@WebMvcTest`), and repository integration tests (`@DataJpaTest`). |

## Project Structure

### Documentation (this feature)

```text
specs/005-games-endpoints-contracts/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 research decisions and rationale
├── data-model.md        # Phase 1 domain, DTO, and database models
├── contracts/           # Phase 1 API contract definitions
│   └── games-api.yaml   # OpenAPI 3.0 specification for Games endpoints
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
│   │   │   │   │   ├── CreateGameCommand.java
│   │   │   │   │   ├── CreateGameUseCase.java
│   │   │   │   │   ├── DeleteGameUseCase.java
│   │   │   │   │   ├── GetGameUseCase.java
│   │   │   │   │   ├── UpdateGameCommand.java
│   │   │   │   │   └── UpdateGameUseCase.java
│   │   │   │   └── out/
│   │   │   │       └── GameRepositoryPort.java
│   │   │   └── service/
│   │   │       └── GameService.java
│   │   ├── domain/
│   │   │   ├── exception/
│   │   │   │   └── GameNotFoundException.java
│   │   │   └── model/
│   │   │       ├── Game.java
│   │   │       └── GameStatus.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/web/
│   │       │   │   ├── GameController.java
│   │       │   │   ├── dto/
│   │       │   │   │   ├── CreateGameRequest.java
│   │       │   │   │   ├── GameResponse.java
│   │       │   │   │   └── UpdateGameRequest.java
│   │       │   │   ├── exception/
│   │       │   │   │   └── GlobalExceptionHandler.java (updated for GameNotFoundException)
│   │       │   │   └── mapper/
│   │       │   │       └── GameWebMapper.java
│   │       │   └── out/persistence/
│   │       │       ├── adapter/
│   │       │       │   └── GameRepositoryAdapter.java
│   │       │       ├── entity/
│   │       │       │   └── GameJpaEntity.java
│   │       │       ├── mapper/
│   │       │       │   └── GamePersistenceMapper.java
│   │       │       └── repository/
│   │       │           └── SpringDataGameRepository.java
│   └── resources/
│       └── db/migration/
│           └── V4__create_games_table.sql
└── test/
    └── java/com/basketapp/bcmserver/
        ├── application/
        │   └── service/
        │       └── GameServiceTest.java
        ├── domain/
        │   └── model/
        │       └── GameTest.java
        └── infrastructure/
            └── adapter/
                ├── in/web/
                │   └── GameControllerTest.java
                └── out/persistence/
                    └── GameRepositoryAdapterTest.java
```

## Complexity Tracking

> **No violations. All architectural boundaries and design patterns fully conform to the Constitution.**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *None* | N/A | N/A |
