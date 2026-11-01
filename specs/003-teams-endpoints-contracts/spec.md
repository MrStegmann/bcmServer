# Feature Specification: Team Contracts and Endpoints

**Feature Branch**: `003-teams-endpoints-contracts`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "The task is to create the endpoints for the Teams table. Whe need to create the CRUD for teams following REST APIs pattern. Each enpoint are private routes and MUST validate the JWT token from the client before start validating the data. The data ALWAYS MUST be validate before is sended to the DB. Use `teams` table defined in `data_models.md`."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticated Team Creation with Pre-DB Validation (Priority: P1)

As an authenticated basketball manager or coach, I want to create a new team by providing its name and optional category, so that I can establish a team profile in the system to which players and games can subsequently be assigned.

**Why this priority**: Team creation is the foundational operational requirement for the team management subsystem. Without creating teams, no roster or match scheduling can exist.

**Independent Test**: Can be independently tested by sending an authenticated request with valid team details to `POST /teams`, verifying the team is persisted and returned with generated metadata (`id`, `created_at`), while unauthenticated requests or invalid payloads are rejected before database invocation.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token submitting valid team data (non-empty `name`, optional `category`), **When** the creation request is received, **Then** the system validates the JWT token first, validates the request payload format second, persists the team in the database, and returns HTTP 201 Created with the full team representation (`id`, `name`, `category`, `created_at`).
2. **Given** a request sent without a JWT token or with an invalid/expired token, **When** attempting to create a team, **Then** the system rejects the request immediately with HTTP 401 Unauthorized without inspecting the payload or querying the database.
3. **Given** an authenticated user submitting a payload with missing, empty, or whitespace-only `name`, **When** the payload is evaluated, **Then** the system rejects the request with HTTP 400 Bad Request detailing the validation violation, with zero database interaction.

---

### User Story 2 - Authenticated Team Listing and Retrieval (Priority: P1)

As an authenticated manager or coach, I want to view a list of all existing teams or retrieve the specific details of a team by its identifier, so that I can inspect team profiles and coordinate league or roster activities.

**Why this priority**: Reading team data is required by client applications to display rosters, match lineups, and team directories.

**Independent Test**: Can be independently tested by making authenticated `GET /teams` and `GET /teams/:id` requests, ensuring existing teams are accurately returned according to the schema contract, and non-existent IDs return HTTP 404.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token, **When** requesting `GET /teams`, **Then** the system returns HTTP 200 OK with a collection of all active teams adhering to the `teams` data model contract.
2. **Given** an authenticated user with a valid JWT token and an existing team with ID `123`, **When** requesting `GET /teams/123`, **Then** the system returns HTTP 200 OK with the team's attributes (`id`, `name`, `category`, `created_at`).
3. **Given** an authenticated user requesting `GET /teams/:id` where the specified team ID does not exist in the database, **When** the query executes, **Then** the system returns HTTP 404 Not Found.
4. **Given** an unauthenticated request to list or fetch a team, **When** evaluated, **Then** the system returns HTTP 401 Unauthorized.

---

### User Story 3 - Authenticated Team Profile Update (Priority: P2)

As an authenticated manager or coach, I want to modify an existing team's name and/or category, so that I can maintain accurate and up-to-date club information when team names or competition categories change.

**Why this priority**: Teams frequently change categories between seasons or refine their display names; updating profiles without losing related players or match history is essential.

**Independent Test**: Can be independently tested by sending authenticated `PUT /teams/:id` or `PATCH /teams/:id` requests with valid modifications, verifying changes are validated before persistence, and verifying updates persist accurately.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an existing team ID, **When** submitting a valid update payload (valid `name` and/or `category`), **Then** the JWT is verified first, payload is validated second, the team record is updated in the database, and HTTP 200 OK is returned with the updated team representation.
2. **Given** an authenticated user attempting to update an existing team with an invalid payload (e.g., blank name), **When** submitted, **Then** the request is rejected with HTTP 400 Bad Request and the database record remains unmodified.
3. **Given** an authenticated update request targeting a non-existent team ID, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
4. **Given** an unauthenticated update request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### User Story 4 - Authenticated Team Deletion (Priority: P2)

As an authenticated club administrator, I want to remove an obsolete or mistakenly created team from the system, so that team registries remain clean and relevant.

**Why this priority**: Data lifecycle hygiene requires administrators to be able to decommission discontinued teams.

**Independent Test**: Can be independently tested by submitting an authenticated `DELETE /teams/:id` request for an existing team, verifying successful deletion (HTTP 204 or HTTP 200), and confirming subsequent retrieval returns HTTP 404.

**Acceptance Scenarios**:

1. **Given** an authenticated user with a valid JWT token and an existing team ID, **When** issuing `DELETE /teams/:id`, **Then** the token is verified, the team is deleted from the system along with cascading associations per data model definitions, and HTTP 204 No Content (or HTTP 200 OK) is returned.
2. **Given** an authenticated delete request for a team ID that does not exist, **When** evaluated, **Then** the system returns HTTP 404 Not Found.
3. **Given** an unauthenticated delete request, **When** submitted, **Then** the system returns HTTP 401 Unauthorized.

---

### Edge Cases

- **Token Validation Precedence**: An unauthenticated request with an invalid payload must receive `401 Unauthorized`, not `400 Bad Request`. Security verification MUST strictly precede data validation.
- **Pre-Database Payload Validation**: Payloads with invalid field types, missing mandatory fields (like empty or missing `name`), or unexpected structural properties must trigger HTTP 400 Bad Request at the boundary without reaching service or persistence layers.
- **Identifier Format Validation**: Route parameters for team IDs (e.g. `/teams/abc` or `/teams/-1`) must be validated as valid positive integer identifiers prior to database lookups, returning HTTP 400 Bad Request if the format is invalid.
- **Nullable and Empty Categories**: A `category` value may be omitted, explicitly `null`, or an empty string; the system must normalize and handle nullable categories consistently without persistence errors.
- **Cascading Integrity on Deletion**: In accordance with `docs/data_models.md`, removing a team triggers cascade deletion of dependent `players` and `games` (`ON DELETE CASCADE`), ensuring database referential integrity is preserved.
- **Whitespace Sanitization**: Team names containing only leading/trailing whitespace must be trimmed and validated against non-empty requirements.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose RESTful CRUD endpoints for the `teams` resource:
  - `POST /teams`: Create a new team
  - `GET /teams`: Retrieve a collection of teams
  - `GET /teams/{id}`: Retrieve a specific team by identifier
  - `PUT /teams/{id}` or `PATCH /teams/{id}`: Update team attributes
  - `DELETE /teams/{id}`: Delete an existing team
- **FR-002**: Every team endpoint MUST be private and require a valid, non-expired JWT Bearer token in the request `Authorization` header.
- **FR-003**: The system MUST enforce a strict request processing order: JWT token authentication MUST be validated first before request payload or parameter validation occurs.
- **FR-004**: Any request lacking a valid JWT token MUST be rejected with HTTP 401 Unauthorized without executing downstream validation, business logic, or database operations.
- **FR-005**: All incoming request payloads (creation, update) and path parameters (team ID) MUST be strictly validated at the controller/entry boundary before passing data to services or repositories.
- **FR-006**: When request validation fails, the system MUST return HTTP 400 Bad Request with standardized error details indicating the invalid attributes, without querying or modifying the database.
- **FR-007**: The system MUST persist and manage teams adhering strictly to the `teams` schema defined in `docs/data_models.md` (`id`, `name`, `category`, `created_at`).
- **FR-008**: Team creation requests MUST require a non-blank `name` and allow an optional `category`.
- **FR-009**: The system MUST automatically assign a unique identifier (`id`) and record creation timestamp (`created_at`) upon team creation.
- **FR-010**: Successful team creation MUST return HTTP 201 Created with the serialized team contract in the response body.
- **FR-011**: Retrieval of a specific team by ID (`GET /teams/{id}`) MUST return HTTP 200 OK with the team data if found, or HTTP 404 Not Found if the team does not exist.
- **FR-012**: Team updates MUST support modifying `name` and `category`, returning HTTP 200 OK with the updated team representation, or HTTP 404 Not Found if the team does not exist.
- **FR-013**: Team deletion (`DELETE /teams/{id}`) MUST permanently delete the team, cascade deletion to associated records per database schema definitions, and return HTTP 204 No Content (or HTTP 200 OK), or HTTP 404 Not Found if the team is absent.
- **FR-014**: All responses MUST adhere to standard HTTP status codes (`201`, `200`, `204`, `400`, `401`, `404`).

### Key Entities *(include if feature involves data)*

- **Team**:
  - `id`: Unique serial/integer identifier of the team (Primary Key).
  - `name`: Non-empty textual name representing the team (e.g., "Cadete A", "Senior Femenino").
  - `category`: Optional textual classification or division (e.g., "U16", "Regional", "Federado").
  - `created_at`: Timestamp recording when the team record was created in the system.
- **Team Create Request**:
  - `name`: String, required, non-blank (e.g., min length 1, max length 100).
  - `category`: String, optional/nullable.
- **Team Update Request**:
  - `name`: String, optional (or required depending on PUT/PATCH semantics), non-blank if present.
  - `category`: String, optional/nullable.
- **Team Response**:
  - `id`: Positive integer.
  - `name`: String.
  - `category`: String (or null).
  - `created_at`: ISO-8601 formatted timestamp string.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of requests to `/teams` endpoints lacking a valid JWT token are rejected with HTTP 401 Unauthorized before any validation or database queries occur.
- **SC-002**: 100% of malformed or invalid request payloads are rejected with HTTP 400 Bad Request at the boundary without triggering database operations.
- **SC-003**: All standard CRUD operations (`POST`, `GET`, `GET by id`, `PUT`/`PATCH`, `DELETE`) conform 100% to REST conventions and return appropriate status codes (201, 200, 204, 400, 401, 404).
- **SC-004**: All database queries and persisted records conform 100% to the `teams` schema defined in `docs/data_models.md`.
- **SC-005**: Team creation, listing, retrieval, update, and deletion operations achieve an average server processing latency of under 100ms under standard operational conditions.

## Assumptions

- **Stateless Authentication**: Client requests provide a signed JWT token in the `Authorization: Bearer <token>` header, verified against existing authentication mechanisms established in the server architecture.
- **Role/Permissions Scope**: In this iteration, any authenticated user possessing a valid JWT token is authorized to access and perform CRUD operations on teams, unless broader RBAC policies are layered in future features.
- **Cascade Behavior**: Deleting a team triggers standard database cascading deletions for child records (`players`, `games`) as documented in `docs/data_models.md`.
- **Pagination & Filtering**: For initial MVP scope, `GET /teams` returns the list of all teams; pagination, sorting, and search filters may be introduced as non-breaking enhancements.
- **Update Protocol**: Both full replacement (`PUT`) or selective updates (`PATCH`) may be supported; the specification requires that modified attributes adhere strictly to boundary validation.

