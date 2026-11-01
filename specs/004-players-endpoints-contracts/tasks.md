# Tasks: Players Contracts and Endpoints

**Input**: Design documents from `specs/004-players-endpoints-contracts/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/players-api.yaml](contracts/players-api.yaml), [quickstart.md](quickstart.md)  
**Tests**: Automated tests are included per Constitution Principle V (Unit testing for domain invariants and use-case services, WebMvc slice tests for controllers, and JPA adapter tests).  
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Exact file paths are specified for every task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and shared error handling infrastructure

- [X] T001 Create Flyway database migration script for players table with team_id foreign key, cascading deletion, and index in src/main/resources/db/migration/V3__create_players_table.sql
- [X] T002 [P] Extend GlobalExceptionHandler to translate PlayerNotFoundException into RFC 7807 404 Not Found ProblemDetail in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/exception/GlobalExceptionHandler.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain model, persistence adapter, shared response DTO, and mappers that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Create Player domain entity with business invariants and sanitization in src/main/java/com/basketapp/bcmserver/domain/model/Player.java
- [X] T004 [P] Create PlayerNotFoundException domain exception in src/main/java/com/basketapp/bcmserver/domain/exception/PlayerNotFoundException.java
- [X] T005 [P] Create unit tests for Player domain entity invariants in src/test/java/com/basketapp/bcmserver/domain/PlayerTest.java
- [X] T006 Define PlayerRepositoryPort outbound port interface in src/main/java/com/basketapp/bcmserver/application/port/out/PlayerRepositoryPort.java
- [X] T007 [P] Create PlayerJpaEntity mapped to PostgreSQL players table in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/PlayerJpaEntity.java
- [X] T008 [P] Create SpringDataPlayerRepository interface extending JpaRepository with findByTeamId in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/SpringDataPlayerRepository.java
- [X] T009 Create MapStruct PlayerPersistenceMapper between Player domain entity and PlayerJpaEntity in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/mapper/PlayerPersistenceMapper.java
- [X] T010 Implement PlayerRepositoryAdapter implementing PlayerRepositoryPort using SpringDataPlayerRepository and PlayerPersistenceMapper in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/PlayerRepositoryAdapter.java
- [X] T011 Create integration/slice test for PlayerRepositoryAdapter in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/PlayerRepositoryAdapterTest.java
- [X] T012 [P] Create PlayerResponse record DTO with OpenAPI annotations in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/PlayerResponse.java
- [X] T013 Create base MapStruct PlayerWebMapper interface with response conversion methods in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/PlayerWebMapper.java

**Checkpoint**: Foundation ready - domain entity, persistence adapter, shared response DTO, and error handling are established. User story implementation can now begin.

---

## Phase 3: User Story 1 - Authenticated Player Creation with Pre-DB Validation (Priority: P1) 🎯 MVP

**Goal**: Authenticated coaches/managers can register a new player to an existing team via `POST /players` and `POST /api/v1/players`. Requests without a valid JWT are rejected immediately with 401 Unauthorized before payload parsing or validation; invalid payloads fail validation with 400 Bad Request before database access; requests referencing a non-existent team ID return 404 Not Found; valid requests persist the player and return 201 Created with Location header and player representation.

**Independent Test**: Send `POST /players` without JWT token (verifies 401 Unauthorized without payload evaluation); send `POST /players` with valid JWT but blank `firstName` or `number` (verifies 400 Bad Request without database write); send `POST /players` with non-existent `teamId` (verifies 404 Not Found); send `POST /players` with valid JWT and valid player details (verifies 201 Created with generated `id`, `created_at`, and `Location` header).

### Tests for User Story 1

- [X] T014 [P] [US1] Create unit tests for player creation logic and team existence verification in PlayerServiceTest in src/test/java/com/basketapp/bcmserver/application/PlayerServiceTest.java
- [X] T015 [P] [US1] Create WebMvc slice tests for POST /players and POST /api/v1/players verifying 201 Created, 400 Bad Request, 401 Unauthorized, and 404 Team Not Found in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerControllerTest.java

### Implementation for User Story 1

- [X] T016 [P] [US1] Create CreatePlayerCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/CreatePlayerCommand.java
- [X] T017 [P] [US1] Create CreatePlayerUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/CreatePlayerUseCase.java
- [X] T018 [P] [US1] Create CreatePlayerRequest web DTO with jakarta validation constraints and OpenAPI annotations in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/CreatePlayerRequest.java
- [X] T019 [US1] Add CreatePlayerRequest to CreatePlayerCommand mapping in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/PlayerWebMapper.java
- [X] T020 [US1] Create PlayerService implementing CreatePlayerUseCase with TeamRepositoryPort verification in src/main/java/com/basketapp/bcmserver/application/service/PlayerService.java
- [X] T021 [US1] Create PlayerController with POST /players and POST /api/v1/players endpoints returning 201 Created with Location header and OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerController.java

**Checkpoint**: User Story 1 is fully functional and independently testable. Player creation MVP is complete.

---

## Phase 4: User Story 2 - Authenticated Player Listing and Retrieval (Priority: P1)

**Goal**: Authenticated coaches/managers can retrieve a list of all players (optionally filtered by `teamId`) via `GET /players` and `GET /api/v1/players`, and retrieve a specific player's profile by identifier via `GET /players/{id}` and `GET /api/v1/players/{id}`. Requests without a valid JWT return 401 Unauthorized; malformed player IDs return 400 Bad Request; non-existent player IDs return 404 Not Found; valid requests return 200 OK with player collections or details matching the schema contract.

**Independent Test**: Send `GET /players` with valid JWT to receive 200 OK with array of all players; send `GET /players?teamId={teamId}` to receive 200 OK filtered by team; send `GET /players/{id}` with existing ID to receive 200 OK with player attributes; send `GET /players/99999` for non-existent ID to receive 404 Not Found; send `GET /players/abc` to receive 400 Bad Request; send requests without JWT to receive 401 Unauthorized.

### Tests for User Story 2

- [X] T022 [P] [US2] Add unit tests for player retrieval and roster listing by team in PlayerServiceTest in src/test/java/com/basketapp/bcmserver/application/PlayerServiceTest.java
- [X] T023 [P] [US2] Add WebMvc slice tests for GET /players, GET /players?teamId={teamId}, and GET /players/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerControllerTest.java

### Implementation for User Story 2

- [X] T024 [P] [US2] Create GetPlayerUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/GetPlayerUseCase.java
- [X] T025 [US2] Implement GetPlayerUseCase methods (findById, findAll, findByTeamId) in PlayerService in src/main/java/com/basketapp/bcmserver/application/service/PlayerService.java
- [X] T026 [US2] Add collection mapping (List<Player> to List<PlayerResponse>) in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/PlayerWebMapper.java
- [X] T027 [US2] Add GET /players (with optional @Positive teamId query parameter) and GET /players/{id} (with @Positive id path parameter) endpoints with OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerController.java

**Checkpoint**: User Stories 1 and 2 are fully functional and independently testable. Players can now be created, listed, and retrieved.

---

## Phase 5: User Story 3 - Authenticated Player Profile Update (Priority: P2)

**Goal**: Authenticated coaches/managers can modify an existing player's attributes (team assignment, first name, last name, jersey number, and jersey name) via `PUT /players/{id}` and `PUT /api/v1/players/{id}`. Token authentication precedes payload validation; invalid payload (e.g., blank first name or blank number) returns 400 Bad Request without updating DB; non-existent player ID returns 404 Not Found; non-existent new team ID returns 404 Not Found; valid payload updates player record and returns 200 OK with updated attributes.

**Independent Test**: Send `PUT /players/{id}` with valid JWT and updated attributes to receive 200 OK with updated representation; send `PUT /players/{id}` with blank `number` to receive 400 Bad Request; send `PUT /players/99999` to receive 404 Not Found; send `PUT /players/{id}` referencing a non-existent `teamId` to receive 404 Not Found; send request without JWT to receive 401 Unauthorized.

### Tests for User Story 3

- [X] T028 [P] [US3] Add unit tests for player update logic, player existence, and team existence verification in PlayerServiceTest in src/test/java/com/basketapp/bcmserver/application/PlayerServiceTest.java
- [X] T029 [P] [US3] Add WebMvc slice tests for PUT /players/{id} and PUT /api/v1/players/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerControllerTest.java

### Implementation for User Story 3

- [X] T030 [P] [US3] Create UpdatePlayerCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/UpdatePlayerCommand.java
- [X] T031 [P] [US3] Create UpdatePlayerUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/UpdatePlayerUseCase.java
- [X] T032 [P] [US3] Create UpdatePlayerRequest web DTO record with jakarta validation constraints and OpenAPI annotations in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/UpdatePlayerRequest.java
- [X] T033 [US3] Add UpdatePlayerRequest to UpdatePlayerCommand mapping in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/PlayerWebMapper.java
- [X] T034 [US3] Implement UpdatePlayerUseCase in PlayerService with TeamRepositoryPort and PlayerRepositoryPort verification in src/main/java/com/basketapp/bcmserver/application/service/PlayerService.java
- [X] T035 [US3] Add PUT /players/{id} and PUT /api/v1/players/{id} endpoints with validation and OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerController.java

**Checkpoint**: User Stories 1, 2, and 3 are fully functional and independently testable. Player profiles can be updated safely.

---

## Phase 6: User Story 4 - Authenticated Player Deletion (Priority: P2)

**Goal**: Authenticated administrators/managers can remove a player from the system via `DELETE /players/{id}` and `DELETE /api/v1/players/{id}`. Requests without JWT return 401 Unauthorized; non-existent player ID returns 404 Not Found; valid requests permanently remove the player, cascade deletion to associated stats records per database schema definitions, and return 204 No Content.

**Independent Test**: Send `DELETE /players/{id}` for an existing player with valid JWT to receive 204 No Content, then verify subsequent `GET /players/{id}` returns 404 Not Found; send `DELETE /players/99999` to receive 404 Not Found; send `DELETE /players/{id}` without JWT to receive 401 Unauthorized.

### Tests for User Story 4

- [X] T036 [P] [US4] Add unit tests for player deletion and existence verification in PlayerServiceTest in src/test/java/com/basketapp/bcmserver/application/PlayerServiceTest.java
- [X] T037 [P] [US4] Add WebMvc slice tests for DELETE /players/{id} and DELETE /api/v1/players/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerControllerTest.java

### Implementation for User Story 4

- [X] T038 [P] [US4] Create DeletePlayerUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/DeletePlayerUseCase.java
- [X] T039 [US4] Implement DeletePlayerUseCase in PlayerService in src/main/java/com/basketapp/bcmserver/application/service/PlayerService.java
- [X] T040 [US4] Add DELETE /players/{id} and DELETE /api/v1/players/{id} endpoints with OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerController.java

**Checkpoint**: All user stories (US1 through US4) are fully functional and independently testable. Full CRUD functionality is complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Security integration verification, OpenAPI contract compliance, and end-to-end quickstart validation across all player endpoints

- [X] T041 Create security integration tests verifying 401 token precedence before validation across all player endpoints in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerSecurityPrecedenceTest.java
- [X] T042 [P] Verify OpenAPI schema and Swagger UI endpoint documentation alignment with specs/004-players-endpoints-contracts/contracts/players-api.yaml
- [X] T043 Execute and validate end-to-end curl verification scenarios documented in specs/004-players-endpoints-contracts/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) - **BLOCKS all user stories**.
- **User Stories (Phase 3+)**: All depend on Foundational (Phase 2) completion:
  - User stories can then proceed sequentially in priority order (US1 → US2 → US3 → US4) or in parallel where resources allow.
  - US1 (Create) establishes `PlayerService` and `PlayerController` base classes.
  - US2 (Read), US3 (Update), and US4 (Delete) extend the established use cases and endpoints.
- **Polish (Phase 7)**: Depends on completion of all user story phases (US1 - US4).

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2 (Foundational) - implements initial `PlayerService` and `PlayerController`.
- **User Story 2 (P1)**: Depends on Phase 2 (Foundational) and US1 base service/controller scaffolding.
- **User Story 3 (P2)**: Depends on Phase 2 (Foundational) and US1 base service/controller scaffolding.
- **User Story 4 (P2)**: Depends on Phase 2 (Foundational) and US1 base service/controller scaffolding.

### Within Each User Story

- Unit/slice tests written first and verified against expected interfaces.
- Commands and use cases before service implementation.
- Request DTOs before mapper and controller implementations.
- Endpoints documented with Springdoc OpenAPI annotations matching `contracts/players-api.yaml`.
- Story completed and verified before proceeding to subsequent stories.

### Parallel Opportunities

- In **Phase 1**: `T002` can run in parallel with `T001`.
- In **Phase 2**: `T004`, `T005`, `T007`, `T008`, and `T012` can run in parallel once their respective interfaces or models are created.
- In **User Story 1 (Phase 3)**: `T014`, `T015`, `T016`, `T017`, and `T018` can all be drafted in parallel before unifying in `T019`-`T021`.
- In **User Story 2 (Phase 4)**: `T022`, `T023`, and `T024` can run in parallel.
- In **User Story 3 (Phase 5)**: `T028`, `T029`, `T030`, `T031`, and `T032` can run in parallel.
- In **User Story 4 (Phase 6)**: `T036`, `T037`, and `T038` can run in parallel.
- In **Polish (Phase 7)**: `T042` can run in parallel with `T041`.

---

## Parallel Execution Examples

### Parallel Example: Foundational Phase
```bash
# Launch domain exception, entity tests, JPA entity, repository interface, and response DTO simultaneously:
Task: "T004 [P] Create PlayerNotFoundException domain exception in src/main/java/com/basketapp/bcmserver/domain/exception/PlayerNotFoundException.java"
Task: "T005 [P] Create unit tests for Player domain entity invariants in src/test/java/com/basketapp/bcmserver/domain/PlayerTest.java"
Task: "T007 [P] Create PlayerJpaEntity mapped to PostgreSQL players table in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/PlayerJpaEntity.java"
Task: "T008 [P] Create SpringDataPlayerRepository interface extending JpaRepository in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/SpringDataPlayerRepository.java"
Task: "T012 [P] Create PlayerResponse record DTO with OpenAPI annotations in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/PlayerResponse.java"
```

### Parallel Example: User Story 1 (Creation)
```bash
# Launch tests, commands, ports, and request DTO together:
Task: "T014 [P] [US1] Create unit tests for player creation logic in src/test/java/com/basketapp/bcmserver/application/PlayerServiceTest.java"
Task: "T015 [P] [US1] Create WebMvc slice tests for POST /players in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/PlayerControllerTest.java"
Task: "T016 [P] [US1] Create CreatePlayerCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/CreatePlayerCommand.java"
Task: "T017 [P] [US1] Create CreatePlayerUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/CreatePlayerUseCase.java"
Task: "T018 [P] [US1] Create CreatePlayerRequest web DTO in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/CreatePlayerRequest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete **Phase 1: Setup** (`T001` - `T002`).
2. Complete **Phase 2: Foundational** (`T003` - `T013`).
3. Complete **Phase 3: User Story 1** (`T014` - `T021`).
4. **STOP and VALIDATE**: Run `PlayerTest`, `PlayerServiceTest`, `PlayerRepositoryAdapterTest`, and `PlayerControllerTest`. Test `POST /players` with curl per Scenario 1-4 in `quickstart.md`.
5. Deploy/demo the core player creation capability (MVP!).

### Incremental Delivery

1. Setup + Foundational → Database table, domain entity, and persistence layer ready.
2. User Story 1 → Player registration enabled with pre-DB validation and team checks (MVP).
3. User Story 2 → Player listing, roster inspection, and player profile retrieval enabled.
4. User Story 3 → Player updates (jersey number changes, name updates, team reassignment) enabled.
5. User Story 4 → Player deletion with database cascading integrity enabled.
6. Polish → Security precedence integration tests, OpenAPI alignment, and end-to-end curl validation complete.

