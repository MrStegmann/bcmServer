# Feature Specification: Users Endpoints & Contracts

**Feature Branch**: `002-users-endpoints-contracts`  
**Created**: 2026-10-29  
**Last Updated**: 2026-11-01  
**Status**: Ready for Review  
**Input**: User description: "The task is to update the error handlers for register and login endpoints: Registration handling errors (400 Bad Request, 409 Conflict, 422 Unprocessable Entity, 429 Too Many Requests, 500 Internal Server Error) and Login handling errors (400 Bad Request, 401 Unauthorized with generic message, 403 Forbidden for locked/inactive/unverified accounts, 429 Too Many Requests for brute-force protection, 500 Internal Server Error for server failures and token generation)."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Public User Registration with Comprehensive Validation and Error Handling (Priority: P1)

As a new visitor to the basketball platform, I want to register a user account with my chosen credentials (such as username/email and password), so that I can establish a recognized identity on the platform while being assured that my data is strictly validated, existing identity collisions are detected, weak inputs receive semantic guidance, and abuse is prevented via rate limiting.

**Why this priority**: Account registration is the primary gateway for user onboarding. Ensuring exhaustive validation and standardized, secure error handling protects platform integrity and guides clients gracefully when registration issues arise.

**Independent Test**: Can be tested independently by submitting valid, invalid, duplicate, and high-frequency registration payloads to the public registration route (`POST /register`), verifying appropriate success statuses (201 Created) and precise error responses (400 Bad Request, 409 Conflict, 422 Unprocessable Entity, 429 Too Many Requests, and 500 Internal Server Error) without leaking sensitive credentials.

**Acceptance Scenarios**:

1. **Successful Registration (201 Created)**:  
   **Given** a new visitor submitting valid credentials (`username`/`email` and password),  
   **When** the registration request is processed,  
   **Then** the password is cryptographically salted and hashed before persistence, a new user record is stored, and the response returns HTTP 201 Created with the public profile (`id`, `username`, `createdAt`) strictly omitting any password data.

2. **Missing Required Fields or Malformed Syntax (400 Bad Request)**:  
   **Given** a registration request missing mandatory fields (e.g., missing `password` or `username`/`email`) or submitting malformed JSON syntax,  
   **When** evaluated at the API boundary,  
   **Then** the request is rejected immediately with HTTP 400 Bad Request and structured problem details before any business logic executes.

3. **Existing Identity Collision (409 Conflict)**:  
   **Given** an existing registered account with an identical username or email address,  
   **When** another registration attempt is submitted with that same identifier,  
   **Then** the system rejects the registration with HTTP 409 Conflict, indicating that the account identifier is already registered.

4. **Semantic Business Validation Failure (422 Unprocessable Entity)**:  
   **Given** a registration request with valid syntax and present fields but violating semantic business rules (e.g., password does not satisfy complexity requirements such as minimum length or character sets),  
   **When** evaluated by domain validation rules,  
   **Then** the system returns HTTP 422 Unprocessable Entity detailing the specific semantic rule violation.

5. **Rate Limiting / Abuse Prevention (429 Too Many Requests)**:  
   **Given** repeated registration requests from a single client IP address exceeding configured thresholds within a time window,  
   **When** an additional registration request is made,  
   **Then** the request is blocked and returns HTTP 429 Too Many Requests with retry information to prevent automated mass account creation.

6. **Unexpected System or Downstream Failure (500 Internal Server Error)**:  
   **Given** an unexpected internal error (such as a database connection outage or internal service crash) during registration processing,  
   **When** the failure occurs,  
   **Then** the system intercepts the exception gracefully and returns HTTP 500 Internal Server Error with a standardized message without exposing internal stack traces.

---

### User Story 2 - Public Stateless User Authentication (Login) with Resilient Error Handling (Priority: P1)

As a registered platform user, I want to authenticate using my credentials and receive a verifiable stateless token, so that I can access protected features securely, while the platform defends against brute-force attacks, protects against account enumeration, and handles account lifecycle states (such as locked or unverified accounts).

**Why this priority**: Authentication gates all secured domain operations. Rigorous status-code semantics and security defenses (anti-enumeration, account locking, rate limiting) are critical to safeguarding user accounts.

**Independent Test**: Can be tested independently by submitting valid credentials, malformed requests, invalid credentials, locked/inactive account credentials, and high-frequency attempts to the public login route (`POST /login`), verifying tokens on success (200 OK) and appropriate error responses (400, 401, 403, 429, 500).

**Acceptance Scenarios**:

1. **Successful Authentication (200 OK)**:  
   **Given** an active registered user submitting correct login credentials,  
   **When** authenticated against the public login endpoint,  
   **Then** the system verifies credentials against the stored hash and returns HTTP 200 OK with a signed stateless authentication token (JWT) and user profile information, without storing server-side session state.

2. **Malformed Payload or Missing Fields (400 Bad Request)**:  
   **Given** a login request with missing credentials (empty or absent username/email or password) or malformed JSON structure,  
   **When** received at the controller boundary,  
   **Then** the system rejects the request with HTTP 400 Bad Request before attempting credential verification.

3. **Invalid Credentials & Enumeration Prevention (401 Unauthorized)**:  
   **Given** a login attempt with an incorrect password or an unregistered username/email,  
   **When** the credentials cannot be authenticated,  
   **Then** the system returns HTTP 401 Unauthorized with a generic message (e.g., "Invalid credentials"), explicitly avoiding revealing whether the account exists or the password was incorrect.

4. **Account Inactive, Locked, or Unverified (403 Forbidden)**:  
   **Given** a user submitting correct credentials for an account that is marked locked, inactive, or pending verification,  
   **When** the account status check is evaluated,  
   **Then** the system denies authentication with HTTP 403 Forbidden, explaining that the account is not eligible to log in.

5. **Brute-Force & Credential Stuffing Throttling (429 Too Many Requests)**:  
   **Given** multiple failed login attempts from an IP address or targeted at a specific account within a limited timeframe,  
   **When** subsequent authentication requests exceed allowable attempt limits,  
   **Then** the system temporarily throttles the client and returns HTTP 429 Too Many Requests with rate-limit headers.

6. **Unexpected Server Failure or Token Generation Fault (500 Internal Server Error)**:  
   **Given** an unexpected internal exception (such as cryptographic token signing failure or unexpected backend failure) during login,  
   **When** the exception occurs,  
   **Then** the system catches the error and returns HTTP 500 Internal Server Error with a safe error payload.

---

### User Story 3 - Authenticated User Profile Retrieval (Priority: P2)

As an authenticated user, I want to fetch my user profile information using my valid authentication token, so that I can inspect my account details within client applications.

**Why this priority**: Users and client applications must be able to retrieve user metadata (identifiers and timestamps) to display accurate profile state.

**Independent Test**: Can be tested independently by presenting a valid authentication token to the user retrieval route (`GET /users/{id}`) and verifying that the user profile attributes match the schema contract, while unauthenticated or unauthorized requests are denied.

**Acceptance Scenarios**:

1. **Given** an authenticated user presenting a valid authentication token, **When** requesting user details for their account, **Then** the system returns HTTP 200 with the user record containing `id`, `username`, and `createdAt`, with the password field explicitly excluded from the response.
2. **Given** a request sent without an authentication token or with an expired/tampered token, **When** requesting user details, **Then** the system denies access with HTTP 401 Unauthorized.
3. **Given** an authenticated user requesting profile details for a user identifier that does not exist in the system, **When** the request is evaluated, **Then** the system returns HTTP 404 Not Found.

---

### User Story 4 - Authenticated User Profile Update (Priority: P2)

As an authenticated user, I want to update my account details (such as changing my username or updating my password), so that my account credentials and preferences stay current and secure.

**Why this priority**: Profile and credential updates are essential for user account maintenance and credential rotation.

**Independent Test**: Can be tested independently by sending authorized update requests (`PUT/PATCH /users/{id}`) with valid and invalid modifications, verifying that updated fields are validated first, password updates are hashed before persistence, duplicate usernames are prevented, and unauthorized callers are blocked.

**Acceptance Scenarios**:

1. **Given** an authenticated user submitting a request to update their username to an available name, **When** the payload passes boundary validation, **Then** the account is updated and the revised profile (`id`, `username`, `createdAt`) is returned without exposing password details.
2. **Given** an authenticated user submitting a new password, **When** the password format passes boundary validation, **Then** the new password is cryptographically hashed before being stored in the database.
3. **Given** an authenticated user attempting to update their username to one that is already claimed by another user, **When** the request is processed, **Then** the update is rejected with HTTP 409 Conflict without modifying the database.
4. **Given** an authenticated user attempting to modify another user's account without administrative privileges, **When** the request is processed, **Then** the system denies the action with HTTP 403 Forbidden.

---

### User Story 5 - Authenticated Account Deletion (Priority: P3)

As an authenticated user, I want to permanently delete my account from the platform, so that my personal data and access credentials are wiped from the system.

**Why this priority**: Allows users to exercise privacy rights and remove unwanted accounts.

**Independent Test**: Can be tested independently by sending an authorized deletion request (`DELETE /users/{id}`) for an existing user account, verifying subsequent authentication or retrieval attempts fail, and confirming that unauthenticated or unauthorized attempts are rejected.

**Acceptance Scenarios**:

1. **Given** an authenticated user requesting deletion of their own account, **When** the request is received and verified, **Then** the user record is permanently removed from the system and HTTP 204 No Content is returned.
2. **Given** a deleted user account, **When** subsequent login or retrieval requests are made with the prior credentials, **Then** the system returns HTTP 401 Unauthorized or HTTP 404 Not Found.
3. **Given** a deletion request with an invalid, expired, or absent authentication token, **When** received, **Then** the system rejects the operation with HTTP 401 Unauthorized.

---

### Edge Cases

- **Missing or Malformed JSON Payloads**: Payloads with invalid syntax, missing root objects, or incompatible field types must be intercepted at the API boundary, returning HTTP 400 Bad Request with standardized error details before executing domain logic.
- **Weak Passwords & Semantic Violations**: Passwords that fail security criteria (e.g. less than 8 characters or missing required complexity) must trigger an HTTP 422 Unprocessable Entity (or 400 depending on endpoint configuration) with descriptive violation messages.
- **Concurrent Identity Registration**: Simultaneous registration attempts using identical identifiers must be handled safely, translating database constraint violations into a clean HTTP 409 Conflict response.
- **Account Enumeration Protection**: Authentication attempts with non-existent usernames/emails must return the exact same HTTP 401 Unauthorized response structure, title, and timing profile as attempts with wrong passwords.
- **Locked, Inactive, or Unverified Accounts**: Accounts that are inactive, locked due to security policy, or pending verification must receive HTTP 403 Forbidden when presenting valid credentials, preventing token issuance.
- **Brute-Force & Credential Stuffing Floods**: Excessive registration or login requests from a single client IP address or targeting a single username must trigger HTTP 429 Too Many Requests with retry headers.
- **Unexpected Downstream or Service Outages**: Unhandled database disconnects, third-party dispatch errors, or token generation exceptions must be captured by global error-handling mechanisms, returning HTTP 500 Internal Server Error without exposing internal system details.
- **Expired, Malformed, or Forged Tokens**: Protected endpoints must reject invalid or expired tokens with HTTP 401 Unauthorized before reaching application controllers.
- **Zero Sensitive Data Exposure**: Under no circumstances (success responses, validation errors, problem details, or system failure logs returned to the user) may plain or hashed passwords be serialized or leaked.
- **Cross-User Access Boundary**: Authenticated users attempting to access, modify, or delete another user's identifier without elevated roles must receive HTTP 403 Forbidden.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose public endpoints for user registration (`POST /register`) and user authentication (`POST /login`) that require no prior authorization header.
- **FR-002**: The system MUST validate incoming registration and login payloads at the boundary, returning HTTP 400 Bad Request with RFC 7807 problem details when required fields (`username`/`email`, `password`) are missing or payload syntax is malformed.
- **FR-003**: The system MUST enforce uniqueness on account identifiers (username and/or email), returning HTTP 409 Conflict if a user attempts to register with an identifier that already exists.
- **FR-004**: The system MUST support semantic validation for business rules (such as minimum password strength criteria), returning HTTP 422 Unprocessable Entity when payload syntax is valid but semantic domain rules are violated.
- **FR-005**: The system MUST enforce rate limiting on registration requests by source IP address, returning HTTP 429 Too Many Requests when request limits are exceeded to prevent automated mass account creation.
- **FR-006**: The system MUST capture unexpected registration failures (such as database unavailability or downstream dispatch errors) and return HTTP 500 Internal Server Error with a standardized error message.
- **FR-007**: The system MUST return HTTP 401 Unauthorized with a generic message (e.g., "Invalid credentials") on login credential mismatches (either unknown user identifier or invalid password) to prevent user enumeration.
- **FR-008**: The system MUST evaluate account state upon successful credential verification, returning HTTP 403 Forbidden if the account is locked, inactive, or pending required verification.
- **FR-009**: The system MUST enforce rate limiting on login attempts per IP address and/or per target account, returning HTTP 429 Too Many Requests when thresholds are breached to mitigate brute-force and credential stuffing attacks.
- **FR-010**: The system MUST safeguard token generation and internal login processing, returning HTTP 500 Internal Server Error if cryptographic token creation or internal services fail unexpectedly.
- **FR-011**: The system MUST cryptographically hash all passwords with a secure salt prior to persisting records, ensuring plain-text passwords are never stored.
- **FR-012**: The system MUST issue signed, stateless JSON Web Tokens (JWT) upon successful authentication and maintain zero server-side session state (`HttpSession`).
- **FR-013**: All response payloads across registration, login, profile retrieval, update, and error flows MUST strictly omit the password field.
- **FR-014**: The system MUST expose protected endpoints for retrieving, updating, and deleting user profiles requiring a valid, unexpired authentication token, returning HTTP 401 Unauthorized for missing or invalid tokens and HTTP 403 Forbidden for unauthorized cross-user modifications.

---

### Key Entities *(include if feature involves data)*

- **User**:
  - `id`: Unique numeric identifier assigned upon creation.
  - `username`: Unique textual username chosen for identification and authentication.
  - `email`: Optional or alternate unique electronic mail identifier.
  - `password`: Cryptographically hashed password stored exclusively in persistence and never exposed.
  - `status`: Account lifecycle state (e.g., `ACTIVE`, `LOCKED`, `INACTIVE`, `UNVERIFIED`).
  - `created_at`: Timestamp indicating account creation.
- **Authentication Credentials**:
  - `username` / `email`: Identifying credential provided by client.
  - `password`: Secret plain-text credential provided in request for immediate validation and comparison.
- **Authentication Result**:
  - `token`: Cryptographically signed JSON Web Token string issued on successful login.
  - `token_type`: Token schema identifier (e.g., `Bearer`).
  - `expires_in`: Duration or expiration timestamp for token validity.
  - `user`: Public profile representation (`id`, `username`, `createdAt`).
- **Problem Detail (Error Response)**:
  - `type`: URI reference identifying problem type.
  - `title`: Short human-readable summary of problem.
  - `status`: HTTP status code (400, 401, 403, 409, 422, 429, 500).
  - `detail`: Human-readable explanation specific to this occurrence.
  - `instance`: URI reference identifying the specific occurrence or endpoint.
  - `invalidParams`: Optional collection of invalid field names and messages for validation errors.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of registration and login requests with missing required fields or malformed syntax return HTTP 400 Bad Request without executing persistence operations.
- **SC-002**: 100% of registration attempts with pre-existing account identifiers return HTTP 409 Conflict with an informative conflict explanation.
- **SC-003**: 100% of registration payloads that fail semantic business rules (e.g., weak passwords) return HTTP 422 Unprocessable Entity detailing the specific failure.
- **SC-004**: 100% of registration or login requests exceeding configured rate limits return HTTP 429 Too Many Requests, maintaining service availability under attack simulation.
- **SC-005**: 100% of login attempts with incorrect credentials return HTTP 401 Unauthorized with identical generic messaging, preventing account enumeration.
- **SC-006**: 100% of login attempts for locked, inactive, or unverified accounts with valid credentials return HTTP 403 Forbidden without issuing authentication tokens.
- **SC-007**: 100% of unexpected internal server failures or token generation faults return HTTP 500 Internal Server Error without leaking internal system traces.
- **SC-008**: 0% of API responses (across registration, login, error bodies, and logs) leak the password attribute or cryptographic salt.
- **SC-009**: 0 server-side session instances (`HttpSession`) are stored on the server during user registration, login, and token-based requests.
- **SC-010**: Valid registration and login requests complete with sub-second response times under standard operational loads.

---

## Assumptions

- **Standard Error Format**: All error responses conform to RFC 7807 Problem Details (`application/problem+json`).
- **Account Identification**: The system supports username-based authentication (and optionally email-based identification if configured), treating identifier uniqueness consistently.
- **Stateless Authentication**: Client applications receive a Bearer JWT upon login and transmit it via the `Authorization: Bearer <token>` header for protected routes.
- **Rate Limiting Strategy**: Rate limiting can be applied via client IP address and/or targeted account identifier, using sliding window or token bucket algorithms with standard HTTP headers (`Retry-After`).
- **Account Status Lifecycle**: If account status attributes (`ACTIVE`, `LOCKED`, `UNVERIFIED`) are not yet stored in a dedicated database column, the system defaults existing users to `ACTIVE` and provides hooks/checks for locking and verification workflows.
- **Password Complexity Rules**: Baseline password complexity requires at least 8 characters with appropriate length boundaries.
- **Persistence Mapping**: User fields map to the database definitions in `docs/data_models.md`.
