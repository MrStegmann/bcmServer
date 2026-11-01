# Feature Specification: Player Stats Contracts and Endpoints

**Feature Branch**: `006-player-stats-endpoints-contracts`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Feature: Player Stats Contracts and Endpoints. Goal: The task is to create the endpoints for the player_stats table. We need to create the CRUD following REST APIs pattern. Each endpoint are private routes and MUST validate the JWT token from the client before start validating the data. The data ALWAYS MUST be validate before sended it to DB. Use player_stats table defined in data_models.md."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticated Player Stats Creation with Pre-DB Validation (Priority: P1)

As an authenticated scorekeeper, statistician, or coach, I want to record game statistics for a specific player in a specific basketball match, so that individual in-game contributions (play time, shot attempts/makes, rebounds, assists, steals, turnovers, fouls, and PIR valuation) are accurately persisted.

**Why this priority**: Creating player statistical records is the core foundation for box scores, individual player evaluations, and post-game performance reporting.

**Independent Test**: Can be tested independently by submitting an authenticated `POST /api/player-stats` (or `POST /player-stats`) request with valid statistic attributes, verifying that the new statistics record is created with a generated identifier, while unauthenticated requests or invalid payloads are rejected before querying or persisting to the database.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token submitting a payload referencing an existing `game_id` and `player_id` with valid numerical statistics (non-negative time, shot metrics, rebounds, assists, steals, turnovers, fouls, and valuation), **When** the creation request is received, **Then** the system validates the JWT token first, validates the request payload second, verifies that both referenced game and player exist, persists the player statistic record, and returns HTTP 201 Created with the serialized record and location header.
2. **Given** a request sent without a JWT token or with an invalid/expired token, **When** attempting to create player statistics, **Then** the system immediately rejects the request with HTTP 401 Unauthorized without evaluating payload fields or accessing the database.
3. **Given** an authenticated user submitting a creation payload with missing mandatory fields (e.g., missing `game_id` or `player_id`) or invalid statistical values (e.g., negative minutes, negative field goal counts, or invalid numeric ranges), **When** evaluated, **Then** the system rejects the request with HTTP 400 Bad Request detailing the specific validation violations without querying or mutating the database.
4. **Given** an authenticated user submitting a creation payload referencing a non-existent `game_id` or `player_id`, **When** evaluated, **Then** the system returns HTTP 404 Not Found indicating the referenced entity does not exist.

---

### User Story 2 - Authenticated Player Stats Listing and Filtered Search (Priority: P1)

As an authenticated coach, analyst, or player, I want to retrieve collections of player stats with optional filtering by game or by player, so that I can analyze full-game box scores, track player progression over time, or view individual match performance.

**Why this priority**: Querying statistics by match or by athlete is critical for generating box scores, team statistics, and player profiles across games.

**Independent Test**: Can be tested independently by submitting authenticated `GET /api/player-stats`, `GET /api/player-stats?gameId={id}`, and `GET /api/player-stats?playerId={id}` requests, verifying that appropriate filtered lists are returned with HTTP 200 OK.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token, **When** requesting `GET /api/player-stats`, **Then** the system returns HTTP 200 OK with the collection of all player statistic records.
2. **Given** an authenticated user providing a `gameId` query parameter (e.g., `GET /api/player-stats?gameId=10`), **When** evaluated, **Then** the system returns HTTP 200 OK containing only statistics for players in that specified game.
3. **Given** an authenticated user providing a `playerId` query parameter (e.g., `GET /api/player-stats?playerId=4`), **When** evaluated, **Then** the system returns HTTP 200 OK containing all recorded match statistics for that designated player.
4. **Given** an authenticated user providing both `gameId` and `playerId` query parameters, **When** evaluated, **Then** the system returns HTTP 200 OK containing the specific matching statistical entry.
5. **Given** an unauthenticated request to retrieve player stats, **When** received, **Then** the system rejects the request immediately with HTTP 401 Unauthorized.

---

### User Story 3 - Authenticated Single Player Stats Retrieval (Priority: P1)

As an authenticated client, I want to fetch a single player statistics record by its unique identifier, so that detailed box-score item information can be displayed or verified.

**Why this priority**: Single-item retrieval is a standard REST contract requirement necessary for deep-linking, auditing, and individual record updates.

**Independent Test**: Can be tested independently by submitting an authenticated `GET /api/player-stats/{id}` request, ensuring that valid IDs return the complete attribute set and non-existent IDs return HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token and an existing player stats ID, **When** requesting `GET /api/player-stats/{id}`, **Then** the system returns HTTP 200 OK with the full player statistics representation.
2. **Given** an authenticated user requesting a player stats ID that does not exist, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
3. **Given** an unauthenticated request to fetch a player stats record by ID, **When** evaluated, **Then** the system rejects the request with HTTP 401 Unauthorized before examining the ID.

---

### User Story 4 - Authenticated Player Stats Modification (Priority: P2)

As an authenticated scorekeeper or statistician, I want to update existing player statistics (e.g., correcting shot attempts, adjusting playing time, updating fouls or PIR valuation), so that errors made during live scorekeeping can be corrected accurately.

**Why this priority**: Post-game review and in-match adjustments frequently require correcting statistical records without recreating the record and losing historical consistency.

**Independent Test**: Can be tested independently by submitting an authenticated `PUT /api/player-stats/{id}` request with updated statistical figures, verifying that the payload is fully validated prior to database access and that updated data is correctly persisted.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an existing player stats identifier, **When** submitting a valid update payload with modified statistics, **Then** the JWT token is validated first, the payload is validated second, the record is updated in the database, and HTTP 200 OK is returned with the updated record.
2. **Given** an authenticated user submitting an update payload containing invalid values (e.g., negative minutes, negative shot counts, or negative fouls), **When** submitted, **Then** the request is rejected with HTTP 400 Bad Request and the database record remains unmodified.
3. **Given** an authenticated update request targeting a non-existent player stats identifier, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
4. **Given** an authenticated update request referencing a non-existent `game_id` or `player_id`, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
5. **Given** an unauthenticated update request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized without inspecting payload data or updating the database.

---

### User Story 5 - Authenticated Player Stats Deletion (Priority: P2)

As an authenticated club administrator or statistician, I want to delete a player statistics record that was logged in error (such as a stat assigned to the wrong player or an abandoned match), so that statistical aggregations and box scores remain clean and accurate.

**Why this priority**: Lifecycle maintenance requires the ability to remove erroneous or duplicate statistical entries.

**Independent Test**: Can be tested independently by submitting an authenticated `DELETE /api/player-stats/{id}` request for an existing record, verifying HTTP 204 No Content (or HTTP 200 OK) response, and verifying subsequent retrieval attempts return HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token and an existing player stats ID, **When** submitting `DELETE /api/player-stats/{id}`, **Then** the JWT token is verified first, the record is removed from the database, and HTTP 204 No Content (or HTTP 200 OK) is returned.
2. **Given** an authenticated delete request for a non-existent player stats ID, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
3. **Given** an unauthenticated delete request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### Edge Cases

- **Security Precedence Over Validation**: If an unauthenticated request contains invalid path variables, query parameters, or malformed JSON payloads, the system MUST return HTTP 401 Unauthorized, never HTTP 400 Bad Request. Security authentication strictly precedes data validation.
- **Pre-Database Input Validation**: Incoming payloads with missing required keys (`game_id`, `player_id`), negative integers for time or counting stats (`minutes`, `seconds`, `t1a`, `t1i`, `t2a`, `t2i`, `t3a`, `t3i`, `oreb`, `dreb`, `asis`, `rec`, `per`, `falt`), or non-integer formats MUST fail validation at the gateway with HTTP 400 Bad Request before reaching the database.
- **Shot Logic Consistency**: While made shots (`t1i`, `t2i`, `t3i`) realistically should not exceed attempted shots (`t1a`, `t2a`, `t3a`), any logical bounds validation MUST trigger HTTP 400 Bad Request if enforced, and never lead to unhandled server errors.
- **Playing Time Boundaries**: `minutes` must be non-negative (>= 0), and `seconds` must be between 0 and 59 inclusive (or non-negative).
- **Valuation (PIR) Value Range**: Unlike counting stats which must be non-negative (>= 0), PIR valuation can be a negative integer (e.g., many turnovers and missed shots with zero points). The valuation field must accept valid signed integer values while defaulting to 0 if omitted.
- **Identifier Format Validation**: Route parameters (such as `/api/player-stats/abc`, `/api/player-stats/-1`, or `/api/player-stats/0`) MUST be validated as positive integers, returning HTTP 400 Bad Request if the format is invalid before querying the database.
- **Foreign Key Referencing**: Creating or updating a player stats record referencing a `game_id` or `player_id` that does not exist in the database MUST return HTTP 404 Not Found without leaking unhandled database exceptions.
- **Cascading Integrity**: Deletion of a referenced `game` or `player` in the system automatically cascades deletion to associated `player_stats` records per database schema definitions (`ON DELETE CASCADE`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose RESTful CRUD endpoints for player statistics under `/api/player-stats` (as well as `/player-stats` and `/api/v1/player-stats`):
  - `POST /api/player-stats`: Create a new player statistic entry
  - `GET /api/player-stats`: List player statistics, supporting optional filtering via `gameId` and `playerId` query parameters
  - `GET /api/player-stats/{id}`: Retrieve a single player statistic entry by its unique identifier
  - `PUT /api/player-stats/{id}`: Update an existing player statistic entry
  - `DELETE /api/player-stats/{id}`: Delete an existing player statistic entry
- **FR-002**: Every player statistics route MUST be private and require a valid, non-expired JWT Bearer token in the request `Authorization` header.
- **FR-003**: The system MUST enforce a strict security execution order: JWT token authentication MUST execute and pass *before* any request body, path parameter, or query parameter validation begins.
- **FR-004**: Any request lacking a valid JWT token MUST be rejected immediately with HTTP 401 Unauthorized without inspecting payload data or querying the database.
- **FR-005**: All incoming request payloads (creation, update) and request parameters (IDs, query filters) MUST be strictly validated at the controller boundary *before* any data is passed to the application services or database layer.
- **FR-006**: When request payload or parameter validation fails, the system MUST return HTTP 400 Bad Request containing structured error details identifying the invalid attributes, without performing any database operation.
- **FR-007**: The system MUST persist and manage player statistic records adhering strictly to the `player_stats` table schema defined in `docs/data_models.md` (`id`, `game_id`, `player_id`, `minutes`, `seconds`, `t1a`, `t1i`, `t2a`, `t2i`, `t3a`, `t3i`, `oreb`, `dreb`, `asis`, `rec`, `per`, `falt`, `valuation`).
- **FR-008**: Player stats creation requests MUST require valid positive integer identifiers for `game_id` and `player_id`. All statistical counters (`minutes`, `seconds`, `t1a`, `t1i`, `t2a`, `t2i`, `t3a`, `t3i`, `oreb`, `dreb`, `asis`, `rec`, `per`, `falt`, `valuation`) MUST default to `0` if omitted in creation payloads.
- **FR-009**: The system MUST verify that the referenced `game_id` exists in the `games` table and `player_id` exists in the `players` table prior to persisting a new or updated record, returning HTTP 404 Not Found if either does not exist.
- **FR-010**: The system MUST automatically assign a unique integer identifier (`id`) upon creation of each player statistics record.
- **FR-011**: Successful creation MUST return HTTP 201 Created with a `Location` header referencing the new resource URI and the full serialized representation of the created entity in the response body.
- **FR-012**: Retrieval of an existing record (`GET /api/player-stats/{id}`) MUST return HTTP 200 OK with the full player statistics representation, or HTTP 404 Not Found if the ID does not exist.
- **FR-013**: Updating a record (`PUT /api/player-stats/{id}`) MUST validate the target ID and update payload, update the record in the database, and return HTTP 200 OK with the updated representation, or HTTP 404 Not Found if the record does not exist.
- **FR-014**: Deleting a record (`DELETE /api/player-stats/{id}`) MUST permanently delete the player statistics record from the database and return HTTP 204 No Content (or HTTP 200 OK), or HTTP 404 Not Found if the record does not exist.
- **FR-015**: All standard and error responses MUST adhere to standard HTTP status codes (`201 Created`, `200 OK`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, and `500 Internal Server Error`) and RFC 7807 Problem Details for errors.

### Key Entities *(include if feature involves data)*

- **PlayerStats**:
  - `id`: Serial integer unique identifier (Primary Key).
  - `game_id`: Integer foreign key referencing `games(id)`.
  - `player_id`: Integer foreign key referencing `players(id)`.
  - `minutes`: Integer play time minutes (non-negative, default 0).
  - `seconds`: Integer play time seconds (0 to 59, default 0).
  - `t1a` / `t1i`: Free throws attempted / made (integers >= 0, default 0).
  - `t2a` / `t2i`: 2-point shots attempted / made (integers >= 0, default 0).
  - `t3a` / `t3i`: 3-point shots attempted / made (integers >= 0, default 0).
  - `oreb` / `dreb`: Offensive / defensive rebounds (integers >= 0, default 0).
  - `asis`: Assists (integers >= 0, default 0).
  - `rec`: Steals / recoveries (integers >= 0, default 0).
  - `per`: Turnovers / losses (integers >= 0, default 0).
  - `falt`: Personal fouls committed (integers >= 0, default 0).
  - `valuation`: Performance Index Rating (PIR) valuation score (signed integer, default 0).
- **CreatePlayerStatsRequest**:
  - `game_id`: Integer, required, positive (> 0).
  - `player_id`: Integer, required, positive (> 0).
  - Statistical fields (`minutes`, `seconds`, `t1a`, `t1i`, `t2a`, `t2i`, `t3a`, `t3i`, `oreb`, `dreb`, `asis`, `rec`, `per`, `falt`, `valuation`): Integers, optional (default 0), must satisfy non-negative constraints (valuation accepts signed integers).
- **UpdatePlayerStatsRequest**:
  - `game_id`: Integer, required, positive (> 0).
  - `player_id`: Integer, required, positive (> 0).
  - Statistical fields: Integers, required, non-negative (valuation accepts signed integers).
- **PlayerStatsResponse**:
  - Complete serialized representation of the player statistics entry including `id`, `game_id`, `player_id`, time metrics, shooting metrics, rebounds, assists, steals, turnovers, fouls, and valuation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of unauthenticated requests to `/api/player-stats` endpoints (missing, malformed, or expired JWT tokens) are rejected with HTTP 401 Unauthorized before any request payload parsing, parameter validation, or database querying occurs.
- **SC-002**: 100% of malformed or invalid request payloads (missing mandatory IDs, negative counters, invalid parameter formats) are rejected with HTTP 400 Bad Request at the controller boundary with zero database queries performed.
- **SC-003**: All five CRUD operations (`POST`, `GET collection`, `GET single`, `PUT`, `DELETE`) fully conform to REST conventions and return appropriate HTTP status codes (201, 200, 204, 400, 401, 404).
- **SC-004**: All persisted player statistics attributes conform 100% to the `player_stats` table schema defined in `docs/data_models.md`.
- **SC-005**: All player stats endpoints maintain an average request processing latency of under 100ms under standard operational conditions.

## Assumptions

- **Stateless Authentication**: Requests provide an authentic JWT Bearer token in the `Authorization: Bearer <token>` header, verified against existing system authentication infrastructure.
- **Role/Permissions Scope**: Any authenticated user possessing a valid JWT token is authorized to perform CRUD operations on player statistics in this phase.
- **Referential Integrity**: Player statistics entries must reference an existing game (`game_id`) and an existing player (`player_id`). Creating or updating records with non-existent foreign keys returns HTTP 404 Not Found.
- **Valuation Calculation**: The `valuation` (PIR) attribute can either be provided directly by the client or calculated using standard FIBA PIR formula: `(points + rebounds + assists + steals + fouls_drawn + blocks) - (missed_field_goals + missed_free_throws + turnovers + fouls_committed)`. Since `docs/data_models.md` defines `valuation INTEGER DEFAULT 0`, the field is stored as provided or defaulted to 0.
- **Route Aliases**: Routes `/api/player-stats`, `/player-stats`, and `/api/v1/player-stats` are supported to ensure compatibility with diverse frontend client routing configurations.
- **Cascade Deletion**: Deletion of a `game` or `player` cascades automatically to associated `player_stats` rows as established in `docs/data_models.md` (`ON DELETE CASCADE`).

