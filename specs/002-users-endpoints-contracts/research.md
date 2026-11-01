# Phase 0 Research & Technical Decisions: Users Endpoints & Contracts

**Feature Branch**: `002-users-endpoints-contracts`  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Password Hashing Strategy

- **Decision**: Use `BCryptPasswordEncoder` (Spring Security Crypto) with a cost factor (strength) of 12.
- **Rationale**: 
  - Standard, battle-tested cryptographic hashing with per-hash automatic salt generation.
  - Highly resilient against rainbow table and brute-force GPU attacks due to its adaptive work factor.
  - Native integration with Spring Security, requiring no external unverified C-bindings or foreign libraries.
  - Meets FR-011 and SC-008 without incurring noticeable latency (< 250ms per hash verification).
- **Alternatives Considered**:
  - *Argon2id*: Excellent memory-hard algorithm, but requires native C library bindings (`de.mkammerer:argon2-jvm`), increasing cross-platform deployment complexity across Windows, Linux, and Alpine Docker containers.
  - *PBKDF2*: Solid standard, but generally less resistant to GPU-accelerated parallel cracking than BCrypt or Argon2.
  - *Plain SHA-256 / SHA-512*: Strictly unacceptable; non-adaptive, unsalted by default, vulnerable to dictionary attacks.

---

## 2. Stateless Authentication & JWT Implementation

- **Decision**: Implement stateless authentication using `io.jsonwebtoken:jjwt-api:0.12.6` (with `jjwt-impl` and `jjwt-jackson` runtime dependencies) in combination with a custom `OncePerRequestFilter` (`JwtAuthenticationFilter`) and Spring Security 6 `SecurityFilterChain` configured with `SessionCreationPolicy.STATELESS`.
- **Rationale**:
  - Meets FR-012 and SC-009: Zero session state stored on the server (`HttpSession` disabled).
  - JJWT 0.12.6 is modern, fully type-safe, supports HMAC-SHA256 (`HS256`) with key validation, and parses claims deterministically.
  - JWT payload encodes:
    - `sub`: User ID (numeric primary key as string)
    - `username`: User login name
    - `iat`: Issue timestamp
    - `exp`: Expiration timestamp (configurable via `jwt.access-token-expiration`, default: 3600000 ms / 1 hour)
  - The JWT secret is externalized via `JWT_SECRET` (min 256 bits / 32 characters) with a development fallback.
- **Alternatives Considered**:
  - *Spring Security OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`)*: Built for external identity providers (Keycloak, Auth0, Okta). Overkill for self-contained username/password authentication where bcmServer is both the token issuer and consumer.
  - *Session-based Cookies*: Violates FR-012 and the Constitution's mandate for stateless server architecture and horizontal scalability.
  - *Database-backed Active Session Tokens*: Adds database lookup overhead on every protected request, violating stateless requirements unless token revocation blacklisting becomes strictly mandatory.

---

## 3. Hexagonal Architecture (Ports and Adapters) Boundaries

- **Decision**: Structure user domain, application use cases, and infrastructure adapters following strict Hexagonal principles (Principle I):
  - **Domain Core (`com.basketapp.bcmserver.domain`)**:
    - `model/User`: Pure Java 21 domain record/class containing `Long id`, `String username`, `String passwordHash`, `UserStatus status`, and `Instant createdAt`.
    - `model/UserStatus`: Enum representing account lifecycle (`ACTIVE`, `LOCKED`, `INACTIVE`, `UNVERIFIED`).
    - `exception/*`: Pure Java exception hierarchy (`DomainException`, `UserAlreadyExistsException`, `UserNotFoundException`, `InvalidCredentialsException`, `AccountStatusException`, `SemanticValidationException`, `RateLimitExceededException`, `UnauthorizedOperationException`).
    - Zero dependencies on Spring, JPA, Hibernate, or Jackson annotations.
  - **Application Ports (`com.basketapp.bcmserver.application.port`)**:
    - *Inbound Ports (`in`)*:
      - `RegisterUserUseCase`: `User register(RegisterUserCommand command)`
      - `LoginUserUseCase`: `AuthenticationResult login(LoginUserCommand command)`
      - `GetUserProfileUseCase`: `User getById(Long id)` / `User getByUsername(String username)`
      - `UpdateUserProfileUseCase`: `User update(UpdateUserCommand command)`
      - `DeleteUserUseCase`: `void delete(Long id, Long currentUserId)`
    - *Outbound Ports (`out`)*:
      - `UserRepositoryPort`: Methods `save(User user)`, `findById(Long id)`, `findByUsername(String username)`, `existsByUsername(String username)`, `deleteById(Long id)`.
      - `PasswordEncoderPort`: `String encode(CharSequence rawPassword)`, `boolean matches(CharSequence rawPassword, String encodedPassword)`.
      - `TokenProviderPort`: `String generateToken(User user)`, `boolean validateToken(String token)`, `Long extractUserId(String token)`, `String extractUsername(String token)`.
      - `RateLimiterPort`: `boolean tryAcquire(String key, int maxRequests, Duration window)`.
  - **Infrastructure Adapters (`com.basketapp.bcmserver.infrastructure.adapter`)**:
    - *Inbound (Web)*:
      - `AuthController`: Exposes `POST /api/v1/auth/register` and `POST /api/v1/auth/login`.
      - `UserController`: Exposes `GET /api/v1/users/{id}`, `GET /api/v1/users/me`, `PUT /api/v1/users/{id}`, and `DELETE /api/v1/users/{id}`.
      - DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UpdateUserRequest`, `UserProfileResponse`.
      - `GlobalExceptionHandler`: Maps domain & framework exceptions to standard RFC 7807 `ProblemDetail`.
    - *Outbound (Persistence)*:
      - `UserJpaEntity`: Annotated with `@Entity`, `@Table(name = "users")`.
      - `SpringDataJpaUserRepository`: Standard Spring Data interface.
      - `UserRepositoryAdapter`: Implements `UserRepositoryPort`, mapping via MapStruct.
    - *Outbound (Security & Rate Limit)*:
      - `BCryptPasswordEncoderAdapter`: Implements `PasswordEncoderPort`.
      - `JwtTokenAdapter`: Implements `TokenProviderPort`.
      - `InMemoryRateLimiterAdapter` (with Redis option): Implements `RateLimiterPort`.
      - `RateLimitingFilter`: Intercepts `/api/v1/auth/register` and `/api/v1/auth/login` to prevent abuse.
- **Rationale**:
  - Decouples core user identity logic from HTTP web details, rate limiting implementation, and PostgreSQL persistence.
  - Enables pure JUnit 5 unit testing for use cases without Spring context bootstrap.
- **Alternatives Considered**:
  - *Standard 3-Tier Layered Architecture (Controller -> Service -> Repository)*: Mixes Spring annotations, database entities, and HTTP concerns into the service layer, violating the project Constitution.

---

## 4. Error Handling Architecture & RFC 7807 Problem Details

- **Decision**: Centralize all error handling in `GlobalExceptionHandler` using Spring 6's standard `org.springframework.http.ProblemDetail` (`application/problem+json`), implementing specific translation rules for every required status code:
  1. **HTTP 400 Bad Request**:
     - *Cause*: Syntax errors, unparseable JSON (`HttpMessageNotReadableException`), type mismatches (`MethodArgumentTypeMismatchException`), missing mandatory parameters, or standard boundary validation errors (`MethodArgumentNotValidException`).
     - *Problem Detail Structure*: `status: 400`, `title: "Bad Request"`, `detail: "..."`, `invalidParams: [...]`.
  2. **HTTP 401 Unauthorized**:
     - *Cause*: Invalid login credentials (unknown user or incorrect password).
     - *Security Policy (Anti-Enumeration)*: Return an identical generic message (`"Invalid credentials"`) and constant timing profile whether the username was not found or the password was incorrect. Never disclose whether an account identifier exists.
  3. **HTTP 403 Forbidden**:
     - *Cause*: Authenticated users attempting cross-user modifications (`UnauthorizedOperationException` / `AccessDeniedException`), OR login attempts for valid credentials where `user.status != UserStatus.ACTIVE` (e.g. `LOCKED`, `INACTIVE`, `UNVERIFIED`) throwing `AccountStatusException`.
     - *Problem Detail Structure*: `status: 403`, `title: "Forbidden"`, `detail: "Account is locked or inactive"`.
  4. **HTTP 409 Conflict**:
     - *Cause*: Identity collision on registration or update (`UserAlreadyExistsException`, or database unique constraint violation).
     - *Problem Detail Structure*: `status: 409`, `title: "Conflict"`, `detail: "Username '...' is already registered"`.
  5. **HTTP 422 Unprocessable Entity**:
     - *Cause*: Syntactically valid payload that violates semantic business or domain rules (`SemanticValidationException`), specifically password complexity constraints (e.g. failing entropy/character variety requirements while satisfying minimum length).
     - *Problem Detail Structure*: `status: 422`, `title: "Unprocessable Entity"`, `detail: "..."`, `invalidParams: [...]`.
  6. **HTTP 429 Too Many Requests**:
     - *Cause*: Exceeding request rate limits on registration (IP-based) or login (IP and target account-based) triggering `RateLimitExceededException`.
     - *Header*: `Retry-After: <seconds>` header included on the HTTP response.
     - *Problem Detail Structure*: `status: 429`, `title: "Too Many Requests"`, `detail: "Rate limit exceeded. Please try again later."`.
  7. **HTTP 500 Internal Server Error**:
     - *Cause*: Unhandled server errors, database connection dropouts, or cryptographic token signing failures (`JwtException`, unexpected `Throwable`).
     - *Safety Rule*: Capture all unexpected exceptions, log the stack trace internally, but return a sanitized generic error to clients without revealing internal system details or traces.
- **Rationale**:
  - Fully aligns with RFC 7807, Spring Boot 3 standards, and Constitution Principle II.
  - Satisfies all criteria in User Stories 1 and 2, edge cases, and success metrics SC-001 through SC-008.

---

## 5. Rate Limiting Strategy (HTTP 429 Prevention)

- **Decision**: Implement a token bucket / sliding window rate limiter behind `RateLimiterPort` and enforce it at the HTTP boundary via `RateLimitingFilter` (or Web Interceptor) on `/api/v1/auth/*`:
  - **Registration Limit**: Max 5 registration requests per minute per IP address.
  - **Login Limit**: Max 5 failed login attempts per minute per IP address / target account username.
  - **Headers**: When rejected, return HTTP 429 with standard header `Retry-After: 60` (or computed backoff).
- **Implementation Mechanism**:
  - In-memory thread-safe `ConcurrentHashMap` with atomic timestamp counters for single-instance / development setups, cleanly swappable with Redis via Spring Data Redis for distributed deployments.
- **Alternatives Considered**:
  - *Bucket4j third-party library*: Effective, but adds an extra dependency. A lightweight sliding window algorithm in `RateLimiterPort` / `RateLimitingFilter` accomplishes the requirement cleanly with zero additional dependencies, maintaining high speed (<1ms).
  - *No rate limiting*: Leaves authentication open to brute force, credential stuffing, and bot registration attacks.

---

## 6. Account Status Lifecycle (HTTP 403 on Login)

- **Decision**: Model `UserStatus` as an enum in the domain layer:
  ```java
  public enum UserStatus {
      ACTIVE,
      LOCKED,
      INACTIVE,
      UNVERIFIED
  }
  ```
  - In `AuthService.login()`:
    1. Authenticate credentials (username + password check).
    2. If credentials match, evaluate `user.getStatus()`.
    3. If status is `LOCKED`, throw `AccountStatusException("Account is locked due to security policy")`.
    4. If status is `INACTIVE` or `UNVERIFIED`, throw `AccountStatusException("Account is inactive or pending verification")`.
    5. If status is `ACTIVE`, proceed to token issuance.
- **Rationale**:
  - Directly fulfills FR-008, User Story 2 Scenario 4, and edge case requirements.
  - Separates credential verification (401) from account authorization status (403).

---

## 7. Database Schema Evolution & Flyway Migration

- **Decision**: Maintain Flyway versioned migrations:
  - `V1__create_users_table.sql`: creates base `users` table (`id`, `username`, `password`, `created_at`).
  - `V2__add_user_status.sql`: adds `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` and creates index `idx_users_status`.
- **Rationale**:
  - Satisfies Constitution Principle III ("Schema migrations MUST be managed exclusively via Flyway versioned migration scripts").
  - Ensures existing records seamlessly default to `'ACTIVE'`.
  - Enforces `spring.jpa.hibernate.ddl-auto: none`.

---

## 8. Summary of Dependencies

| Dependency | Purpose | Scope |
| :--- | :--- | :--- |
| `org.springframework.boot:spring-boot-starter-web` | Spring MVC REST APIs & ProblemDetail | Compile |
| `org.springframework.boot:spring-boot-starter-security` | Security filters, authentication, BCrypt | Compile |
| `io.jsonwebtoken:jjwt-api:0.12.6` | JJWT API for stateless token operations | Compile |
| `io.jsonwebtoken:jjwt-impl:0.12.6` | JJWT runtime implementation | Runtime |
| `io.jsonwebtoken:jjwt-jackson:0.12.6` | JJWT Jackson JSON serialization | Runtime |
| `org.mapstruct:mapstruct:1.6.3` | Compile-time model & DTO mappers | Compile |
| `org.mapstruct:mapstruct-processor:1.6.3` | Annotation processor for MapStruct | Provided / Processor |
| `org.flywaydb:flyway-core` | Versioned SQL database migration | Compile |
| `org.flywaydb:flyway-database-postgresql` | PostgreSQL database support for Flyway | Runtime |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver | Runtime |
| `org.springframework.boot:spring-boot-starter-validation` | Jakarta Validation annotations & validator | Compile |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4` | OpenAPI 3 documentation & Swagger UI | Compile |
| `org.springframework.security:spring-security-test` | Security test utilities | Test |
| `org.springframework.boot:spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, MockMvc | Test |
