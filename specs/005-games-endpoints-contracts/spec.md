# Feature Specification: Games Contracts and Endpoints

**Feature Branch**: `005-games-endpoints-contracts`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Feature: GamesContracts and Endpoints. Goal: The task is to create the endpoints for the games table. We need to create the CRUD following REST APIs pattern. Each endpoint are private routes and MUST validate the JWT token from the client before start validating the data. The data ALWAYS MUST be validate before sended it to DB. Use games table defined in data_models.md."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticated Game Creation with Pre-DB Validation (Priority: P1)

As an authenticated basketball coach or club manager, I want to schedule and record a new basketball game for a team by specifying the opposing team, tournament round, match date, and optional initial status or scores, so that our team can track fixtures, plan match events, and record game progress.

**Why this priority**: Game scheduling and creation is the foundational operational requirement for match tracking. Without creating game records, coaches cannot manage live match tracking, update scores, or link player statistics to specific games.

**Independent Test**: Can be independently tested by sending an authenticated creation request with valid game data to `POST /api/games` (or `POST /games`), confirming that the game record is persisted and returned with its unique identifier and generated creation timestamp, while unauthenticated requests or invalid payloads are rejected before querying or modifying the database.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token submitting valid game data (valid `team_id`, non-empty `opponent`, optional or valid `round`, non-empty `date`, and optional `status`), **When** the creation request is received, **Then** the system validates the JWT token first, validates the request payload format second, verifies the referenced team exists, persists the game record with appropriate defaults (scores and fouls set to 0, status defaulting to `pending` if unspecified, round defaulting to `'1'` if unspecified), and returns HTTP 201 Created with the full game representation.
2. **Given** a request sent without a JWT token or with an expired/invalid token, **When** attempting to create a game, **Then** the system immediately rejects the request with HTTP 401 Unauthorized without inspecting the payload or querying the database.
3. **Given** an authenticated user submitting a creation payload with missing or invalid required fields (e.g., missing `team_id`, blank `opponent`, blank `date`, or negative score values), **When** the payload is evaluated, **Then** the system rejects the request with HTTP 400 Bad Request detailing the specific validation violations, without invoking any database query or mutation.
4. **Given** an authenticated user submitting a valid creation payload referencing a `team_id` that does not exist in the database, **When** the request is processed, **Then** the system returns HTTP 404 Not Found indicating the referenced team does not exist.

---

### User Story 2 - Authenticated Game Listing and Retrieval (Priority: P1)

As an authenticated coach, manager, or player, I want to retrieve a list of all games (with optional filtering by team) or view the complete details and score breakdown of a single game by its unique identifier, so that I can review upcoming fixtures, ongoing matches, and historical results.

**Why this priority**: Retrieving game lists and single match summaries is necessary for displaying schedules, checking quarter-by-quarter scoreboards, and feeding match viewers in client applications.

**Independent Test**: Can be independently tested by making authenticated `GET /api/games` and `GET /api/games/:id` requests, ensuring existing game records return complete quarter scores and foul breakdowns, while non-existent IDs return HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token, **When** requesting `GET /api/games`, **Then** the system returns HTTP 200 OK with the collection of game records adhering to the data model contract.
2. **Given** an authenticated user with a valid JWT token providing a `teamId` query parameter (e.g., `GET /api/games?teamId=5`), **When** the request is evaluated, **Then** the system returns HTTP 200 OK containing only games associated with that specified team.
3. **Given** an authenticated user with a valid JWT token and an existing game identifier, **When** requesting `GET /api/games/:id`, **Then** the system returns HTTP 200 OK with the complete game attributes including quarter scores (`result_c1` through `result_c4`, `result_extra`, and opponent equivalents) and foul counts.
4. **Given** an authenticated user requesting `GET /api/games/:id` where the game identifier does not exist, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
5. **Given** an unauthenticated request to list or fetch a game, **When** evaluated, **Then** the system immediately returns HTTP 401 Unauthorized.

---

### User Story 3 - Authenticated Game Modification and Score/Status Updates (Priority: P2)

As an authenticated scorekeeper or coach, I want to update game attributes (such as date, round, status, quarter-by-quarter scores, and foul counts), so that live and post-game statistics accurately reflect what transpired on the court.

**Why this priority**: Matches transition through states (`pending` -> `live` -> `finished`) and accumulate scores and fouls across four quarters and potential overtime. Updating game data is vital for real-time score keeping and official record keeping.

**Independent Test**: Can be independently tested by submitting an authenticated `PUT /api/games/:id` request with updated score/status data, verifying that the payload is validated prior to database access, and verifying that the database record is updated accurately.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an existing game identifier, **When** submitting a valid update payload with modified match status, scores, or fouls, **Then** the JWT token is validated first, the payload is validated second, the game record is updated, and HTTP 200 OK is returned with the updated game representation.
2. **Given** an authenticated user submitting an update payload containing invalid values (e.g., negative score, negative fouls, invalid match status, or blank opponent), **When** submitted, **Then** the request is rejected with HTTP 400 Bad Request and the database record remains unmodified.
3. **Given** an authenticated update request targeting a non-existent game identifier, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
4. **Given** an authenticated update request referencing a new `team_id` that does not exist in the database, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
5. **Given** an unauthenticated update request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized without inspecting the payload or updating the database.

---

### User Story 4 - Authenticated Game Deletion (Priority: P2)

As an authenticated club administrator, I want to delete a game record that was cancelled or created in error, so that match schedules and team records remain accurate and tidy.

**Why this priority**: Lifecycle maintenance requires the ability to safely remove erroneously scheduled or duplicate games along with their associated match data.

**Independent Test**: Can be independently tested by sending an authenticated `DELETE /api/games/:id` request for an existing game, verifying that deletion returns HTTP 204 No Content (or HTTP 200 OK), and confirming that subsequent retrieval attempts return HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token and an existing game identifier, **When** submitting `DELETE /api/games/:id`, **Then** the JWT token is validated first, the game record is removed from the database along with cascading associations, and HTTP 204 No Content (or HTTP 200 OK) is returned.
2. **Given** an authenticated delete request for a game identifier that does not exist in the database, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
3. **Given** an unauthenticated delete request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### Edge Cases

- **Token Verification Precedence**: If an unauthenticated request contains malformed JSON or invalid parameter formats, the system MUST return HTTP 401 Unauthorized, never HTTP 400 Bad Request. Security validation strictly precedes data validation.
- **Pre-Database Payload Validation**: Payloads with invalid field types, missing mandatory fields (`team_id`, `opponent`, `date`), invalid status strings (values other than `pending`, `live`, `finished`), or negative scores/fouls MUST fail validation with HTTP 400 Bad Request before hitting the database.
- **Identifier Format Validation**: Route parameters for game identifiers (e.g., `/api/games/invalid`, `/api/games/-5`, `/api/games/0`) MUST be validated as valid positive integer identifiers prior to database lookups, returning HTTP 400 Bad Request if the format is malformed.
- **Foreign Key Referencing**: Attempting to create or update a game with a `team_id` that does not exist in the `teams` table MUST return HTTP 404 Not Found (or HTTP 400 Bad Request specifying that the referenced team does not exist) without exposing unhandled database errors.
- **Score and Foul Boundaries**: Quarter scores (`result_c1` through `result_c4`, `result_extra`, and opponent scores) and foul counts (`fouls_c1` through `fouls_c4`, `fouls_extra`, and opponent fouls) must be non-negative integers (>= 0).
- **Default Field Values**: When optional score or foul fields are omitted in creation payloads, the system MUST default each of them to 0. Similarly, if `status` is omitted, it MUST default to `pending`, and if `round` is omitted, it MUST default to `'1'`.
- **Cascading Integrity on Deletion**: Per `docs/data_models.md`, deleting a game cascades deletions to dependent records in `player_stats` and `match_cache` (`ON DELETE CASCADE`), ensuring database referential integrity.
- **Whitespace Sanitization**: Text fields (`opponent`, `round`, `date`) containing leading or trailing whitespace must be trimmed and cannot consist exclusively of whitespace characters.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose RESTful CRUD endpoints for the `games` resource under `/api/games` (as well as `/games`):
  - `POST /api/games`: Create a new game record
  - `GET /api/games`: Retrieve a collection of games (supporting optional `teamId` filtering)
  - `GET /api/games/{id}`: Retrieve a specific game by unique identifier
  - `PUT /api/games/{id}`: Update an existing game's attributes, scores, fouls, and status
  - `DELETE /api/games/{id}`: Delete an existing game record
- **FR-002**: Every game endpoint MUST be private and require a valid, non-expired JWT Bearer token in the request `Authorization` header.
- **FR-003**: The system MUST enforce a strict security execution order: JWT token verification MUST execute and succeed *before* any request body, path parameter, or query parameter validation takes place.
- **FR-004**: Any request lacking a valid JWT token MUST be rejected immediately with HTTP 401 Unauthorized without inspecting payload data or querying the database.
- **FR-005**: All incoming request payloads (creation, update) and route parameters (game ID, filter parameters) MUST be strictly validated at the entry boundary *before* any data reaches application domain or database layers.
- **FR-006**: When request payload or parameter validation fails, the system MUST return HTTP 400 Bad Request accompanied by descriptive error details identifying the invalid fields, with zero database interaction.
- **FR-007**: The system MUST persist and manage game records adhering strictly to the `games` schema defined in `docs/data_models.md` (`id`, `team_id`, `opponent`, `round`, `date`, `status`, `created_at`, plus own/opponent score and foul quarter fields).
- **FR-008**: Game creation requests MUST require a valid positive integer `team_id`, non-blank `opponent`, and non-blank `date`. If omitted, `round` defaults to `'1'`, `status` defaults to `'pending'`, and all quarter score and foul fields default to `0`.
- **FR-009**: The system MUST automatically assign a unique identifier (`id`) and record the creation timestamp (`created_at`) upon game creation.
- **FR-010**: Successful game creation MUST return HTTP 201 Created with the full representation of the created game in the response body.
- **FR-011**: Retrieval of an existing game by ID (`GET /api/games/{id}`) MUST return HTTP 200 OK with the full game details, or HTTP 404 Not Found if the game does not exist.
- **FR-012**: Game updates (`PUT /api/games/{id}`) MUST validate the target ID and update payload, update the game in the database, and return HTTP 200 OK with the updated game representation, or HTTP 404 Not Found if the game does not exist.
- **FR-013**: Game deletion (`DELETE /api/games/{id}`) MUST permanently remove the game record, cascade deletion to associated records per data model constraints, and return HTTP 204 No Content (or HTTP 200 OK), or HTTP 404 Not Found if the game does not exist.
- **FR-014**: All error and status responses MUST adhere to standard HTTP status codes (`201 Created`, `200 OK`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, and `500 Internal Server Error`).

### Key Entities *(include if feature involves data)*

- **Game**:
  - `id`: Unique serial/integer identifier of the game (Primary Key).
  - `team_id`: Integer foreign key referencing the associated home team in `teams(id)`.
  - `opponent`: Non-empty text representing the name of the opposing team.
  - `round`: Non-empty text representing tournament/league round (defaults to `'1'`).
  - `date`: Non-empty text representing match date/time (ISO-8601 or custom string format).
  - `status`: Text match state (allowed values: `pending`, `live`, `finished`; defaults to `pending`).
  - `created_at`: Timestamp recording when the game was created.
  - **Scores (Own Team)**: `result_c1`, `result_c2`, `result_c3`, `result_c4`, `result_extra` (non-negative integers, default 0).
  - **Scores (Opponent)**: `result_c1_opponent`, `result_c2_opponent`, `result_c3_opponent`, `result_c4_opponent`, `result_extra_opponent` (non-negative integers, default 0).
  - **Fouls (Own Team)**: `fouls_c1`, `fouls_c2`, `fouls_c3`, `fouls_c4`, `fouls_extra` (non-negative integers, default 0).
  - **Fouls (Opponent)**: `fouls_c1_opponent`, `fouls_c2_opponent`, `fouls_c3_opponent`, `fouls_c4_opponent`, `fouls_extra_opponent` (non-negative integers, default 0).
- **Game Create Request**:
  - `team_id`: Integer, required, positive (> 0).
  - `opponent`: String, required, non-blank.
  - `round`: String, optional (defaults to `'1'`), non-blank if provided.
  - `date`: String, required, non-blank.
  - `status`: String, optional (defaults to `'pending'`), one of `pending`, `live`, `finished`.
  - Scores and fouls: Integers, optional (default 0), must be >= 0 if provided.
- **Game Update Request**:
  - `team_id`: Integer, required, positive (> 0).
  - `opponent`: String, required, non-blank.
  - `round`: String, required, non-blank.
  - `date`: String, required, non-blank.
  - `status`: String, required, one of `pending`, `live`, `finished`.
  - Scores and fouls: Integers, required, must be >= 0.
- **Game Response**:
  - Full game representation with all schema attributes serialized, including `id`, `team_id`, `opponent`, `round`, `date`, `status`, `created_at`, all quarter scores, and all quarter fouls.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of unauthenticated requests to `/api/games` endpoints (missing, malformed, or expired JWT tokens) are rejected with HTTP 401 Unauthorized before any payload validation or database operations take place.
- **SC-002**: 100% of malformed or invalid request payloads (missing required attributes, negative score/foul counts, invalid status values) are rejected with HTTP 400 Bad Request at the boundary without reaching the database.
- **SC-003**: All five CRUD operations (`POST`, `GET`, `GET by id`, `PUT`, `DELETE`) fully conform to REST conventions and return appropriate HTTP status codes (201, 200, 204, 400, 401, 404).
- **SC-004**: All persisted game attributes conform 100% to the `games` table schema defined in `docs/data_models.md`.
- **SC-005**: Game creation, retrieval, update, and deletion operations maintain an average processing latency under 100ms under standard operational conditions.

## Assumptions

- **Stateless Authentication**: Requests provide an authentic JWT Bearer token in the `Authorization: Bearer <token>` header, verified against existing system authentication infrastructure.
- **Role/Permissions Scope**: Any authenticated user possessing a valid JWT token is authorized to access and perform CRUD operations on games in this phase.
- **Team Referential Integrity**: Games must be associated with an existing team in the `teams` table (`team_id` foreign key). Creating a game with a non-existent `team_id` returns HTTP 404 Not Found.
- **Status Enum Values**: Allowed game status values are strictly `pending`, `live`, and `finished`, reflecting standard match lifecycle progression.
- **Cascade Behavior**: Deletion of a game cascades to dependent records (`player_stats`, `match_cache`) according to `docs/data_models.md`.
- **Path Aliasing**: Both `/api/games` and `/games` base routes are supported for client compatibility.

