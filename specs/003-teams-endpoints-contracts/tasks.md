# Tasks: Team Contracts and Endpoints

**Input**: Design documents from `specs/003-teams-endpoints-contracts/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/teams-api.yaml](contracts/teams-api.yaml)  
**Tests**: Automated tests are included per Constitution Principle V (Unit testing for domain/services, Slice tests for controllers, and JPA adapter tests).  
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Exact file paths are specified for every task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and shared error handling infrastructure

- [X] T001 Create Flyway database migration script for teams table in src/main/resources/db/migration/V2__create_teams_table.sql
- [X] T002 [P] Extend GlobalExceptionHandler to translate MethodArgumentTypeMismatchException and ConstraintViolationException into RFC 7807 400 Bad Request ProblemDetail in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/exception/GlobalExceptionHandler.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain model, persistence adapter, and shared mapping that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Create Team domain entity with business invariants and sanitization in src/main/java/com/basketapp/bcmserver/domain/model/Team.java
- [X] T004 [P] Create TeamNotFoundException domain exception in src/main/java/com/basketapp/bcmserver/domain/exception/TeamNotFoundException.java
- [X] T005 [P] Add TeamNotFoundException handler to GlobalExceptionHandler in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/exception/GlobalExceptionHandler.java
- [X] T006 [P] Create unit tests for Team domain entity invariants in src/test/java/com/basketapp/bcmserver/domain/TeamTest.java
- [X] T007 Define TeamRepositoryPort outbound port interface in src/main/java/com/basketapp/bcmserver/application/port/out/TeamRepositoryPort.java
- [X] T008 [P] Create TeamJpaEntity mapped to PostgreSQL teams table in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/TeamJpaEntity.java
- [X] T009 [P] Create SpringDataTeamRepository interface extending JpaRepository in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/SpringDataTeamRepository.java
- [X] T010 Create MapStruct TeamPersistenceMapper between Team domain entity and TeamJpaEntity in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/mapper/TeamPersistenceMapper.java
- [X] T011 Implement TeamRepositoryAdapter implementing TeamRepositoryPort using SpringDataTeamRepository and TeamPersistenceMapper in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/TeamRepositoryAdapter.java
- [X] T012 Create integration/slice test for TeamRepositoryAdapter in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/TeamRepositoryAdapterTest.java
- [X] T013 [P] Create TeamResponse record DTO in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/TeamResponse.java
- [X] T014 Create base MapStruct TeamWebMapper interface for web DTO conversions in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/TeamWebMapper.java

**Checkpoint**: Foundation ready - domain, persistence adapter, shared response DTO, and error handling are established. User story implementation can now begin.

---

## Phase 3: User Story 1 - Authenticated Team Creation with Pre-DB Validation (Priority: P1) 🎯 MVP

**Goal**: Authenticated coaches/managers can create a new team via `POST /teams` and `POST /api/v1/teams`. Requests without a valid JWT are rejected immediately with 401 Unauthorized before payload parsing or validation; invalid payloads fail validation with 400 Bad Request before database access; valid requests persist the team and return 201 Created with Location header and team representation.

**Independent Test**: Send `POST /teams` without JWT token (verifies 401 without payload evaluation); send `POST /teams` with valid JWT but blank `name` (verifies 400 Bad Request without database write); send `POST /teams` with valid JWT and valid team details (verifies 201 Created with generated `id`, `created_at`, and `Location` header).

### Tests for User Story 1

- [X] T015 [P] [US1] Create unit tests for team creation logic in TeamService in src/test/java/com/basketapp/bcmserver/application/TeamServiceTest.java
- [X] T016 [P] [US1] Create WebMvc slice tests for POST /teams and POST /api/v1/teams in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamControllerTest.java

### Implementation for User Story 1

- [X] T017 [P] [US1] Create CreateTeamCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/CreateTeamCommand.java
- [X] T018 [P] [US1] Create CreateTeamUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/CreateTeamUseCase.java
- [X] T019 [P] [US1] Create CreateTeamRequest web DTO with jakarta validation constraints in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/CreateTeamRequest.java
- [X] T020 [US1] Add CreateTeamRequest to CreateTeamCommand mapping in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/TeamWebMapper.java
- [X] T021 [US1] Create TeamService implementing CreateTeamUseCase in src/main/java/com/basketapp/bcmserver/application/service/TeamService.java
- [X] T022 [US1] Create TeamController with POST /teams and POST /api/v1/teams endpoints returning 201 Created with Location header and OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamController.java

**Checkpoint**: User Story 1 is fully functional and independently testable. Team creation MVP is complete.

---

## Phase 4: User Story 2 - Authenticated Team Listing and Retrieval (Priority: P1)

**Goal**: Authenticated coaches/managers can retrieve a list of all active teams via `GET /teams` and fetch specific team details by identifier via `GET /teams/{id}`. Requests without a valid JWT return 401 Unauthorized; malformed team IDs return 400 Bad Request; non-existent team IDs return 404 Not Found; valid requests return 200 OK.

**Independent Test**: Send `GET /teams` with valid JWT to receive 200 OK with array of teams; send `GET /teams/{id}` with existing ID to receive 200 OK with team attributes; send `GET /teams/9999` for non-existent ID to receive 404 Not Found; send `GET /teams/invalid-id` to receive 400 Bad Request; send requests without JWT to receive 401 Unauthorized.

### Tests for User Story 2

- [X] T023 [P] [US2] Add unit tests for team retrieval and listing in TeamService in src/test/java/com/basketapp/bcmserver/application/TeamServiceTest.java
- [X] T024 [P] [US2] Add WebMvc slice tests for GET /teams and GET /teams/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamControllerTest.java

### Implementation for User Story 2

- [X] T025 [P] [US2] Create GetTeamUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/GetTeamUseCase.java
- [X] T026 [US2] Implement GetTeamUseCase methods (findById, findAll) in TeamService in src/main/java/com/basketapp/bcmserver/application/service/TeamService.java
- [X] T027 [US2] Add collection mapping (List<Team> to List<TeamResponse>) in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/TeamWebMapper.java
- [X] T028 [US2] Add GET /teams and GET /teams/{id} endpoints with @Positive path variable validation and OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamController.java

**Checkpoint**: User Stories 1 and 2 are fully functional and independently testable. Teams can now be created, listed, and retrieved.

---

## Phase 5: User Story 3 - Authenticated Team Profile Update (Priority: P2)

**Goal**: Authenticated coaches/managers can modify an existing team's name and/or category via `PUT /teams/{id}` and `PUT /api/v1/teams/{id}`. Token authentication precedes payload validation; invalid payload (blank name) returns 400 Bad Request without updating DB; non-existent ID returns 404 Not Found; valid payload updates team and returns 200 OK with updated attributes.

**Independent Test**: Send `PUT /teams/{id}` with valid JWT and updated name/category to receive 200 OK with updated representation; send `PUT /teams/{id}` with blank name to receive 400 Bad Request; send `PUT /teams/9999` to receive 404 Not Found; send request without JWT to receive 401 Unauthorized.

### Tests for User Story 3

- [X] T029 [P] [US3] Add unit tests for team update logic in TeamService in src/test/java/com/basketapp/bcmserver/application/TeamServiceTest.java
- [X] T030 [P] [US3] Add WebMvc slice tests for PUT /teams/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamControllerTest.java

### Implementation for User Story 3

- [X] T031 [P] [US3] Create UpdateTeamCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/UpdateTeamCommand.java
- [X] T032 [P] [US3] Create UpdateTeamUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/UpdateTeamUseCase.java
- [X] T033 [P] [US3] Create UpdateTeamRequest web DTO with validation annotations in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/UpdateTeamRequest.java
- [X] T034 [US3] Add UpdateTeamRequest to UpdateTeamCommand mapping in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/mapper/TeamWebMapper.java
- [X] T035 [US3] Implement UpdateTeamUseCase in TeamService in src/main/java/com/basketapp/bcmserver/application/service/TeamService.java
- [X] T036 [US3] Add PUT /teams/{id} and PUT /api/v1/teams/{id} endpoints with validation and OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamController.java

**Checkpoint**: User Stories 1, 2, and 3 are fully functional and independently testable. Team profiles can be modified safely.

---

## Phase 6: User Story 4 - Authenticated Team Deletion (Priority: P2)

**Goal**: Authenticated administrators can remove obsolete teams via `DELETE /teams/{id}` and `DELETE /api/v1/teams/{id}`. Requests without JWT return 401 Unauthorized; non-existent ID returns 404 Not Found; valid requests delete the team, cascade deletion to associated records per database schema, and return 204 No Content.

**Independent Test**: Send `DELETE /teams/{id}` for an existing team with valid JWT to receive 204 No Content, then verify subsequent `GET /teams/{id}` returns 404 Not Found; send `DELETE /teams/9999` to receive 404 Not Found; send `DELETE /teams/{id}` without JWT to receive 401 Unauthorized.

### Tests for User Story 4

- [X] T037 [P] [US4] Add unit tests for team deletion in TeamService in src/test/java/com/basketapp/bcmserver/application/TeamServiceTest.java
- [X] T038 [P] [US4] Add WebMvc slice tests for DELETE /teams/{id} in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamControllerTest.java

### Implementation for User Story 4

- [X] T039 [P] [US4] Create DeleteTeamUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/DeleteTeamUseCase.java
- [X] T040 [US4] Implement DeleteTeamUseCase in TeamService in src/main/java/com/basketapp/bcmserver/application/service/TeamService.java
- [X] T041 [US4] Add DELETE /teams/{id} and DELETE /api/v1/teams/{id} endpoints with OpenAPI documentation in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamController.java

**Checkpoint**: All user stories (US1 through US4) are fully functional and independently testable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: System integration verification, security audit, and documentation validation across all stories

- [X] T042 Create comprehensive security integration tests verifying 401 token precedence before validation across all team endpoints in src/test/java/com/basketapp/bcmserver/infrastructure/config/SecurityConfigTest.java
- [X] T043 [P] Verify OpenAPI schema and Swagger UI endpoint documentation alignment with specs/003-teams-endpoints-contracts/contracts/teams-api.yaml
- [X] T044 Execute and validate end-to-end curl verification scenarios documented in specs/003-teams-endpoints-contracts/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories
- **User Story 1 (Phase 3 - P1)**: Depends on Phase 2 completion - Delivers core MVP
- **User Story 2 (Phase 4 - P1)**: Depends on Phase 2 completion (integrates alongside US1 in TeamService/TeamController)
- **User Story 3 (Phase 5 - P2)**: Depends on Phase 2 completion (integrates alongside US1/US2 in TeamService/TeamController)
- **User Story 4 (Phase 6 - P2)**: Depends on Phase 2 completion (integrates alongside US1/US2/US3 in TeamService/TeamController)
- **Polish (Phase 7)**: Depends on completion of all user stories

### User Story Dependencies

- **User Story 1 (P1)**: Can start immediately after Foundational (Phase 2) is complete. Creates `TeamService` and `TeamController`.
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) is complete. Adds query ports and endpoints to `TeamService` and `TeamController`.
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) is complete. Adds update command, port, and endpoints to `TeamService` and `TeamController`.
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) is complete. Adds delete port and endpoints to `TeamService` and `TeamController`.

### Within Each User Story

1. Write tests first and ensure they fail before implementation
2. Inbound command and port definitions
3. Web DTOs and MapStruct mapping rules
4. Application service implementation
5. Controller endpoints with validation and OpenAPI documentation
6. Verify story test passes independently before moving to next story

### Parallel Opportunities

- **Phase 1**: T001 and T002 can run in parallel
- **Phase 2**:
  - T003, T004, T006 (domain model and tests) can run in parallel with T008, T009 (JPA entity and repo interface)
  - T005 (exception handler) and T013 (TeamResponse DTO) can run in parallel
  - Once T008-T010 are done, T011 and T012 can run
- **Phase 3 (US1)**:
  - T015 (service test) and T016 (controller test) can run in parallel
  - T017 (command), T018 (port), T019 (request DTO) can be created in parallel
- **Phase 4 (US2)**:
  - T023 (service test) and T024 (controller test) can run in parallel
  - T025 (port) can run in parallel with tests
- **Phase 5 (US3)**:
  - T029 (service test) and T030 (controller test) can run in parallel
  - T031 (command), T032 (port), T033 (request DTO) can be created in parallel
- **Phase 6 (US4)**:
  - T037 (service test) and T038 (controller test) can run in parallel
  - T039 (port) can run in parallel with tests
- **Phase 7 (Polish)**:
  - T042 and T043 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch test tasks for User Story 1 in parallel:
Task T015: "Create unit tests for team creation logic in TeamService in src/test/java/com/basketapp/bcmserver/application/TeamServiceTest.java"
Task T016: "Create WebMvc slice tests for POST /teams and POST /api/v1/teams in src/test/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/TeamControllerTest.java"

# Launch command, port, and request DTO for User Story 1 in parallel:
Task T017: "Create CreateTeamCommand application command record in src/main/java/com/basketapp/bcmserver/application/port/in/CreateTeamCommand.java"
Task T018: "Create CreateTeamUseCase inbound port interface in src/main/java/com/basketapp/bcmserver/application/port/in/CreateTeamUseCase.java"
Task T019: "Create CreateTeamRequest web DTO with jakarta validation constraints in src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/dto/CreateTeamRequest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (Migration script `V2__create_teams_table.sql` and global validation handling)
2. Complete Phase 2: Foundational (Domain `Team`, `TeamRepositoryPort`, `TeamRepositoryAdapter`, `TeamJpaEntity`, `TeamResponse`)
3. Complete Phase 3: User Story 1 (`POST /teams` creation with JWT auth precedence and validation)
4. **STOP and VALIDATE**: Run `TeamControllerTest` and test `POST /teams` with curl
5. Deploy/demo the Team Creation MVP!

### Incremental Delivery

1. Setup + Foundational → Solid hexagonal foundation verified
2. Add User Story 1 (Creation) → Verify independently → Deliver MVP
3. Add User Story 2 (Listing & Retrieval) → Verify independently → Deliver Read operations
4. Add User Story 3 (Update) → Verify independently → Deliver Team mutation capabilities
5. Add User Story 4 (Deletion) → Verify independently → Deliver full CRUD lifecycle
6. Run Phase 7 Polish & Quickstart → Final security & contract sign-off

---

## Notes

- All tasks follow strict format: `- [ ] [TaskID] [P?] [Story?] Description with file path`
- Every user story has explicit independent test criteria
- Zero framework or JPA dependencies leak into domain model (`Team.java`)
- Token validation precedence (401 before 400) is guaranteed via Spring Security filter chain and verified in tests
- Inbound validation (`@NotBlank`, `@Size`, `@Positive`) produces standard RFC 7807 `ProblemDetail` responses

