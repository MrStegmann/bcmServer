# Implementation Plan: Players Contracts and Endpoints

**Branch**: `004-players-endpoints-contracts` | **Date**: 2026-10-31 | **Spec**: [specs/004-players-endpoints-contracts/spec.md](spec.md)

**Input**: Feature specification from `/specs/004-players-endpoints-contracts/spec.md`

## Summary

Implement private, RESTful CRUD endpoints for the `players` resource (`POST /players`, `GET /players`, `GET /players/{id}`, `PUT /players/{id}`, `DELETE /players/{id}`) backed by PostgreSQL and managed by Flyway (`V3__create_players_table.sql`). The implementation strictly enforces Hexagonal Architecture (Ports and Adapters), stateless JWT authentication precedence (401 Unauthorized before any validation or database query), pre-database payload/parameter validation (`jakarta.validation` producing RFC 7807 `ProblemDetail`), compile-time MapStruct mapping, and Springdoc OpenAPI documentation.

## Technical Context

**Language/Version**: Java 21 (LTS, records, sealed interfaces, pattern matching)  
**Primary Dependencies**: Spring Boot 3.4.2 (Spring Web MVC, Spring Security 6.x, Spring Data JPA, Spring Validation), JJWT 0.12.6, MapStruct 1.6.3, Springdoc OpenAPI 2.8.4  
**Storage**: PostgreSQL (persisted via Spring Data JPA and Flyway migration `V3__create_players_table.sql` matching `docs/data_models.md`)  
**Testing**: JUnit 5, Mockito, Spring Boot Test Slice (`@WebMvcTest`, `@DataJpaTest`), Spring Security Test  
**Target Platform**: Linux/Containerized JVM (Docker Compose local runtime)  
**Project Type**: Web Service (Hexagonal RESTful API)  
**Performance Goals**: <100ms average server processing latency for CRUD operations under standard load  
**Constraints**: 
- Strict token validation precedence (401 Unauthorized before request payload evaluation or DB query)
- 100% pre-database validation on all inbound payloads, query parameters, and path parameters (RFC 7807 400 Bad Request)
- Zero framework or persistence annotation leakage into the domain core
- Pure Java 21 domain entities without Spring or JPA dependencies
- Team existence verification (`TeamRepositoryPort.existsById`) prior to player creation or update
- All cross-layer mapping performed via MapStruct compile-time mappers
**Scale/Scope**: Player roster management for basketball teams with jersey number, personal info, team linkage, and stats cascading

## Constitution Check

*GATE: Evaluated before Phase 0 and re-evaluated post Phase 1 design.*

| Principle | Gate Status | Compliance Details |
| :--- | :---: | :--- |
| **I. Hexagonal Architecture (Ports and Adapters)** | **PASS** | `Player` domain entity is pure Java 21 in `domain.model`. Inbound ports (`CreatePlayerUseCase`, `GetPlayerUseCase`, `UpdatePlayerUseCase`, `DeletePlayerUseCase`) and outbound port (`PlayerRepositoryPort`) isolate business logic. Adapters (`PlayerController`, `PlayerRepositoryAdapter`) encapsulate Web and JPA respectively. Cross-layer mapping is handled via MapStruct (`PlayerWebMapper`, `PlayerPersistenceMapper`). Persistence entities never leak to the web layer. |
| **II. API Design & Documentation-First** | **PASS** | RESTful HTTP verbs and status codes (201, 200, 204, 400, 401, 404). Documented with Springdoc OpenAPI annotations and standalone OpenAPI spec contract in `contracts/players-api.yaml`. Boundary validation using `jakarta.validation` producing RFC 7807 `ProblemDetail`. |
| **III. Data Persistence & Flyway** | **PASS** | Schema evolution managed strictly via Flyway migration `V3__create_players_table.sql` matching `docs/data_models.md`. Hibernate `ddl-auto` remains `none` or `validate`. JPA entity isolated in persistence adapter. Foreign key referential integrity on `team_id` with `ON DELETE CASCADE`. |
| **IV. Stateless Security & JWT** | **PASS** | Endpoints are protected by stateless JWT Bearer token authentication in `SecurityConfig`. Filter chain enforces token validation before controller/payload validation. No session state. |
| **V. Automated Testing & Quality** | **PASS** | Comprehensive testing planned across architectural layers: unit tests for domain `Player` and application `PlayerService` (pure JUnit 5/Mockito), slice tests for `PlayerController` (`@WebMvcTest`), and repository integration tests (`@DataJpaTest`). |

## Project Structure

### Documentation (this feature)

```text
specs/004-players-endpoints-contracts/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 research decisions and rationale
├── data-model.md        # Phase 1 domain, DTO, and database models
├── contracts/           # Phase 1 API contract definitions
│   └── players-api.yaml # OpenAPI 3.0 specification for Players endpoints
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
│   │   │   │   │   ├── CreatePlayerCommand.java
│   │   │   │   │   ├── CreatePlayerUseCase.java
│   │   │   │   │   ├── DeletePlayerUseCase.java
│   │   │   │   │   ├── GetPlayerUseCase.java
│   │   │   │   │   ├── UpdatePlayerCommand.java
│   │   │   │   │   └── UpdatePlayerUseCase.java
│   │   │   │   └── out/
│   │   │   │       └── PlayerRepositoryPort.java
│   │   │   └── service/
│   │   │       └── PlayerService.java
│   │   ├── domain/
│   │   │   ├── exception/
│   │   │   │   └── PlayerNotFoundException.java
│   │   │   └── model/
│   │   │       └── Player.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/web/
│   │       │   │   ├── PlayerController.java
│   │       │   │   ├── dto/
│   │       │   │   │   ├── CreatePlayerRequest.java
│   │       │   │   │   ├── PlayerResponse.java
│   │       │   │   │   └── UpdatePlayerRequest.java
│   │       │   │   ├── exception/
│   │       │   │   │   └── GlobalExceptionHandler.java (updated for PlayerNotFoundException)
│   │       │   │   └── mapper/
│   │       │   │       └── PlayerWebMapper.java
│   │       │   └── out/persistence/
│   │       │       ├── adapter/
│   │       │       │   └── PlayerRepositoryAdapter.java
│   │       │       ├── entity/
│   │       │       │   └── PlayerJpaEntity.java
│   │       │       ├── mapper/
│   │       │       │   └── PlayerPersistenceMapper.java
│   │       │       └── repository/
│   │       │           └── SpringDataPlayerRepository.java
│   │       └── config/
│   │           └── SecurityConfig.java (existing - covers /players endpoints)
│   └── resources/
│       └── db/migration/
│           └── V3__create_players_table.sql
└── test/
    └── java/com/basketapp/bcmserver/
        ├── application/service/
        │   └── PlayerServiceTest.java
        ├── domain/model/
        │   └── PlayerTest.java
        └── infrastructure/adapter/
            ├── in/web/
            │   └── PlayerControllerTest.java
            └── out/persistence/
                └── PlayerRepositoryAdapterTest.java
```

**Structure Decision**: The chosen single-project modular architecture organizes code by Hexagonal architectural layers (`domain`, `application`, `infrastructure`), strictly isolating domain rules from frameworks. MapStruct bridges Web DTOs, domain models, and JPA entities with compile-time type safety.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations identified. The design strictly adheres to all constitutional principles.*
