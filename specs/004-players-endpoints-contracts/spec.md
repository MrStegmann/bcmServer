# Feature Specification: Players Contracts and Endpoints

**Feature Branch**: `004-players-endpoints-contracts`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "The task is to create the endpoints for the `players` table. Whe need to create the CRUD for players following REST APIs pattern. Each enpoint are private routes and MUST validate the JWT token from the client before start validating the data. The data ALWAYS MUST be validate before is sended to the DB. Use `players` table defined in `data_models.md`."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticated Player Creation with Pre-DB Validation (Priority: P1)

As an authenticated basketball manager or coach, I want to register a new player to an existing team by providing their team ID, first name, last name, jersey number, and optional jersey name, so that the player is added to the team roster and can participate in matches and statistics tracking.

**Why this priority**: Player creation is the core operational entry point for roster management. Without player registration, games and player statistics cannot be linked to actual roster members.

**Independent Test**: Can be independently tested by sending an authenticated request with valid player data to `POST /players`, verifying that the player is persisted in the database and returned with generated metadata (`id`, `created_at`), while unauthenticated requests or invalid payloads are rejected before any database invocation.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token submitting valid player data (existing `team_id`, non-empty `first_name`, non-empty `last_name`, non-empty `number`, and optional `jersey_name`), **When** the creation request is received, **Then** the system validates the JWT token first, validates the request payload format second, verifies the associated team exists, persists the player in the database, and returns HTTP 201 Created with the full player representation (`id`, `team_id`, `first_name`, `last_name`, `number`, `jersey_name`, `created_at`).
2. **Given** a request sent without a JWT token or with an invalid/expired token, **When** attempting to create a player, **Then** the system immediately rejects the request with HTTP 401 Unauthorized without inspecting the payload or querying the database.
3. **Given** an authenticated user submitting a payload with missing required fields (e.g., missing `first_name`, missing or negative `team_id`, blank `number`), **When** the payload is evaluated, **Then** the system rejects the request with HTTP 400 Bad Request detailing the specific validation violations, with zero database interaction.
4. **Given** an authenticated user submitting a valid payload referencing a `team_id` that does not exist in the database, **When** the request is processed, **Then** the system returns HTTP 404 Not Found indicating the team does not exist.

---

### User Story 2 - Authenticated Player Listing and Retrieval (Priority: P1)

As an authenticated manager or coach, I want to retrieve a list of all players (optionally filtered by team) or view the detailed profile of an individual player by their identifier, so that I can inspect player assignments, jersey numbers, and personal details.

**Why this priority**: Viewing player data is essential for displaying rosters in the client application, managing game lineups, and checking player profiles.

**Independent Test**: Can be independently tested by making authenticated `GET /players` and `GET /players/:id` requests, ensuring existing player records are accurately returned according to the schema contract, and non-existent IDs return HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token, **When** requesting `GET /players`, **Then** the system returns HTTP 200 OK with a collection of all players adhering to the `players` data model contract.
2. **Given** an authenticated user with a valid JWT token providing a `teamId` query parameter (e.g., `GET /players?teamId=10`), **When** the request is processed, **Then** the system returns HTTP 200 OK with the collection of players belonging specifically to that team.
3. **Given** an authenticated user with a valid JWT token and an existing player with ID `456`, **When** requesting `GET /players/456`, **Then** the system returns HTTP 200 OK with the player's attributes (`id`, `team_id`, `first_name`, `last_name`, `number`, `jersey_name`, `created_at`).
4. **Given** an authenticated user requesting `GET /players/:id` where the specified player ID does not exist in the database, **When** the query executes, **Then** the system returns HTTP 404 Not Found.
5. **Given** an unauthenticated request to list or fetch a player, **When** evaluated, **Then** the system returns HTTP 401 Unauthorized.

---

### User Story 3 - Authenticated Player Profile Update (Priority: P2)

As an authenticated manager or coach, I want to modify an existing player's details (such as changing their jersey number, correcting their name, updating their jersey display name, or reassigning them to a different team), so that roster information remains accurate and up to date over the course of the season.

**Why this priority**: Player information changes frequently across seasons (e.g., jersey number changes, team promotions, name corrections); maintaining up-to-date data without losing historical stats is crucial.

**Independent Test**: Can be independently tested by sending authenticated `PUT /players/:id` or `PATCH /players/:id` requests with valid modifications, verifying that updates are validated prior to persistence, and verifying changes persist accurately.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an existing player ID, **When** submitting a valid update payload (valid `team_id`, `first_name`, `last_name`, `number`, and/or `jersey_name`), **Then** the JWT is verified first, payload is validated second, the player record is updated in the database, and HTTP 200 OK is returned with the updated player representation.
2. **Given** an authenticated user attempting to update an existing player with an invalid payload (e.g., blank `first_name` or blank `number`), **When** submitted, **Then** the request is rejected with HTTP 400 Bad Request and the database record remains unmodified.
3. **Given** an authenticated update request targeting a non-existent player ID, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
4. **Given** an authenticated update request referencing a new `team_id` that does not exist in the database, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
5. **Given** an unauthenticated update request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### User Story 4 - Authenticated Player Deletion (Priority: P2)

As an authenticated club administrator or manager, I want to remove a player who has left the club or was registered by mistake, so that team rosters remain clean and compliant.

**Why this priority**: Data lifecycle management requires administrators to remove deactivated or erroneously created player records.

**Independent Test**: Can be independently tested by submitting an authenticated `DELETE /players/:id` request for an existing player, verifying successful deletion (HTTP 204 No Content), and confirming subsequent retrieval returns HTTP 404 Not Found.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token and an existing player ID, **When** issuing `DELETE /players/:id`, **Then** the token is verified first, the player record is permanently removed from the database along with cascading associations per data model definitions, and HTTP 204 No Content is returned.
2. **Given** an authenticated delete request for a player ID that does not exist, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
3. **Given** an unauthenticated delete request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### Edge Cases

- **Token Validation Precedence**: An unauthenticated request with an invalid or malformed payload must receive HTTP 401 Unauthorized, never HTTP 400 Bad Request. Security verification MUST strictly precede data validation.
- **Pre-Database Payload Validation**: Payloads with invalid field types, missing required attributes (`team_id`, `first_name`, `last_name`, `number`), or structural anomalies must trigger HTTP 400 Bad Request at the web boundary before reaching service or persistence layers.
- **Foreign Key Validation**: Attempting to create or update a player referencing a non-existent `team_id` must be detected and return HTTP 404 Not Found (or HTTP 400 Bad Request with a clear message indicating invalid team reference) without unhandled database constraint exceptions.
- **Identifier Format Validation**: Route parameters for player IDs (e.g., `/players/abc`, `/players/-1`, `/players/0`) must be validated as valid positive integer identifiers prior to database lookups, returning HTTP 400 Bad Request if the format is invalid.
- **Nullable and Optional Jersey Name**: The `jersey_name` field may be omitted or explicitly `null`; if present, it must be properly trimmed and persisted without validation errors.
- **Jersey Number Formatting**: Jersey numbers are defined as text to support values like "00", "0", "7", "23"; numbers must not be empty or blank whitespace.
- **Cascading Integrity on Deletion**: In accordance with `docs/data_models.md`, deleting a player cascades to dependent records in `player_stats` (`ON DELETE CASCADE`), ensuring database referential integrity is preserved.
- **Whitespace Sanitization**: Names and numbers containing leading or trailing whitespace must be sanitized or rejected if they consist solely of blank characters.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose RESTful CRUD endpoints for the `players` resource:
  - `POST /players`: Create a new player record
  - `GET /players`: Retrieve a collection of players (with optional `teamId` filtering)
  - `GET /players/{id}`: Retrieve a specific player by unique identifier
  - `PUT /players/{id}` or `PATCH /players/{id}`: Update an existing player's attributes
  - `DELETE /players/{id}`: Delete an existing player record
- **FR-002**: Every player endpoint MUST be private and require a valid, non-expired JWT Bearer token in the request `Authorization` header.
- **FR-003**: The system MUST enforce a strict request processing order: JWT token authentication MUST be validated first before request payload or parameter validation occurs.
- **FR-004**: Any request lacking a valid JWT token MUST be rejected with HTTP 401 Unauthorized without executing downstream validation, business logic, or database operations.
- **FR-005**: All incoming request payloads (creation, update) and path parameters (player ID) MUST be strictly validated at the controller/entry boundary before passing data to domain services or persistence adapters.
- **FR-006**: When request validation fails, the system MUST return HTTP 400 Bad Request with standardized error details indicating the invalid attributes, without querying or mutating the database.
- **FR-007**: The system MUST persist and manage players adhering strictly to the `players` schema defined in `docs/data_models.md` (`id`, `team_id`, `first_name`, `last_name`, `number`, `jersey_name`, `created_at`).
- **FR-008**: Player creation requests MUST require valid positive integer `team_id`, non-blank `first_name`, non-blank `last_name`, non-blank `number`, and allow an optional `jersey_name`.
- **FR-009**: The system MUST automatically assign a unique identifier (`id`) and record the creation timestamp (`created_at`) upon player creation.
- **FR-010**: Successful player creation MUST return HTTP 201 Created with the serialized player representation in the response body.
- **FR-011**: Retrieval of a specific player by ID (`GET /players/{id}`) MUST return HTTP 200 OK with the player data if found, or HTTP 404 Not Found if the player does not exist.
- **FR-012**: Player updates MUST support modifying `team_id`, `first_name`, `last_name`, `number`, and `jersey_name`, returning HTTP 200 OK with the updated player representation, or HTTP 404 Not Found if the player does not exist.
- **FR-013**: Player deletion (`DELETE /players/{id}`) MUST permanently delete the player, cascade deletion to associated `player_stats` records per database schema definitions, and return HTTP 204 No Content (or HTTP 200 OK), or HTTP 404 Not Found if the player is absent.
- **FR-014**: All responses MUST adhere to standard HTTP status codes (`201`, `200`, `204`, `400`, `401`, `404`).

### Key Entities *(include if feature involves data)*

- **Player**:
  - `id`: Unique serial/integer identifier of the player (Primary Key).
  - `team_id`: Integer foreign key referencing the associated team in `teams(id)`.
  - `first_name`: Non-empty textual first name (e.g., "Michael").
  - `last_name`: Non-empty textual last name (e.g., "Jordan").
  - `number`: Non-empty textual jersey/dorsal number (e.g., "23", "00", "7").
  - `jersey_name`: Optional textual name printed on the jersey (e.g., "JORDAN").
  - `created_at`: Timestamp recording when the player record was created in the system.
- **Player Create Request**:
  - `team_id`: Integer, required, positive (> 0).
  - `first_name`: String, required, non-blank (e.g., min length 1, max length 100).
  - `last_name`: String, required, non-blank (e.g., min length 1, max length 100).
  - `number`: String, required, non-blank (e.g., min length 1, max length 10).
  - `jersey_name`: String, optional/nullable (e.g., max length 50).
- **Player Update Request**:
  - `team_id`: Integer, required (or optional in PATCH), positive if present.
  - `first_name`: String, required (or optional in PATCH), non-blank if present.
  - `last_name`: String, required (or optional in PATCH), non-blank if present.
  - `number`: String, required (or optional in PATCH), non-blank if present.
  - `jersey_name`: String, optional/nullable.
- **Player Response**:
  - `id`: Positive integer.
  - `team_id`: Positive integer.
  - `first_name`: String.
  - `last_name`: String.
  - `number`: String.
  - `jersey_name`: String (or null).
  - `created_at`: ISO-8601 formatted timestamp string.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of requests to `/players` endpoints lacking a valid JWT token are rejected with HTTP 401 Unauthorized before any validation or database queries occur.
- **SC-002**: 100% of malformed or invalid request payloads are rejected with HTTP 400 Bad Request at the boundary without triggering database operations.
- **SC-003**: All standard CRUD operations (`POST`, `GET`, `GET by id`, `PUT`/`PATCH`, `DELETE`) conform 100% to REST conventions and return appropriate status codes (201, 200, 204, 400, 401, 404).
- **SC-004**: All database queries and persisted records conform 100% to the `players` schema defined in `docs/data_models.md`.
- **SC-005**: Player creation, listing, retrieval, update, and deletion operations achieve an average server processing latency of under 100ms under standard operational conditions.

## Assumptions

- **Stateless Authentication**: Client requests provide a signed JWT token in the `Authorization: Bearer <token>` header, verified against existing authentication mechanisms established in the server architecture.
- **Role/Permissions Scope**: In this iteration, any authenticated user possessing a valid JWT token is authorized to access and perform CRUD operations on players, unless broader RBAC policies are layered in future features.
- **Team Dependency**: A player must be associated with an existing team in the `teams` table; creating a player requires a valid `team_id`.
- **Cascade Behavior**: Deleting a player triggers standard database cascading deletions for child records (`player_stats`) as documented in `docs/data_models.md`.
- **Filtering & Listing**: `GET /players` supports returning all players or filtering by `teamId` via query parameter; pagination and complex search can be added as non-breaking enhancements.
- **Update Protocol**: Both full replacement (`PUT`) or selective updates (`PATCH`) may be supported; the specification requires that modified attributes adhere strictly to boundary validation.

