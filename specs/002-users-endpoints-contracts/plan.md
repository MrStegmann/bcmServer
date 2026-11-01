# Implementation Plan: Users Endpoints & Contracts

**Branch**: `002-users-endpoints-contracts` | **Date**: 2026-11-01 | **Spec**: [specs/002-users-endpoints-contracts/spec.md](file:///j:/GitHub/BasketApp/bcmServer/specs/002-users-endpoints-contracts/spec.md)

**Input**: Feature specification from `/specs/002-users-endpoints-contracts/spec.md` updated for comprehensive error handling across public registration and login endpoints.

## Summary

Implement and harden secure, stateless REST endpoints for user registration, authentication (login), retrieval, update, and deletion in the `bcmServer` Spring Boot application. Strictly adhere to Hexagonal Architecture (Ports and Adapters). Ensure passwords are cryptographically hashed using BCrypt before persistence, authentication issues stateless signed JWT tokens (JJWT 0.12.6) with zero server session retention, and compile-time object mapping uses MapStruct to guarantee that password hashes never leak into domain models or API responses.

A primary focus is the implementation of comprehensive, RFC 7807-compliant error handling and abuse prevention:
- **Registration Endpoint (`POST /api/v1/auth/register`)**:
  - `400 Bad Request`: Boundary syntax validation failures, missing mandatory fields (`username`, `password`), or malformed JSON payloads.
  - `409 Conflict`: Existing account identifier collisions (duplicate username or email).
  - `422 Unprocessable Entity`: Semantic business validation failures (e.g. passwords failing complexity rules).
  - `429 Too Many Requests`: IP-based rate limiting / abuse mitigation with `Retry-After` header.
  - `500 Internal Server Error`: Graceful capture of unhandled server, database, or downstream exceptions with sanitized problem details.
- **Login Endpoint (`POST /api/v1/auth/login`)**:
  - `400 Bad Request`: Missing credentials or malformed JSON syntax.
  - `401 Unauthorized`: Generic anti-enumeration response ("Invalid credentials") for wrong passwords and unregistered accounts alike.
  - `403 Forbidden`: Account lifecycle gating for locked, inactive, or unverified accounts.
  - `429 Too Many Requests`: Brute-force throttling and credential-stuffing prevention per IP and target account.
  - `500 Internal Server Error`: Interception of cryptographic token generation faults and internal server failures.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK 21)  
**Primary Dependencies**:
- `spring-boot-starter-web` (Spring Boot 3.4.2)
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `springdoc-openapi-starter-webmvc-ui` (Springdoc 2.8.4)
- `io.jsonwebtoken:jjwt-api:0.12.6` (with `jjwt-impl` and `jjwt-jackson` runtime)
- `org.mapstruct:mapstruct:1.6.3` (with `mapstruct-processor:1.6.3`)
- `org.flywaydb:flyway-core` & `org.flywaydb:flyway-database-postgresql`
- `org.postgresql:postgresql` (PostgreSQL JDBC driver)

**Storage**: PostgreSQL 16 (table `users` via Flyway migrations; `ddl-auto: none`)  
**Testing**: JUnit 5, Mockito, Spring Boot Test (`@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`), `spring-security-test`  
**Target Platform**: Multi-platform JVM (Linux server, macOS, Windows, Docker container)  
**Project Type**: Spring Boot Web Service / REST API application  
**Performance Goals**:
- Registration and login latency < 250ms under normal load
- Rate limiting check latency < 2ms per request
- Cold startup and database connection verification in < 15 seconds
- Fail-fast database connectivity timeout in < 10 seconds

**Constraints**:
- Strict Hexagonal Architecture: Zero Spring/JPA/web dependencies in domain core
- Complete server statelessness: 0 `HttpSession` instances; token validation per request
- Zero plain-text passwords: 100% hashed via BCrypt before storage; 0% password leakage in responses
- Boundary validation: All payloads validated via `jakarta.validation` prior to domain logic
- Flyway schema management: `users` table created and evolved via versioned SQL migrations
- RFC 7807 standard: All error responses returned with `Content-Type: application/problem+json`

**Scale/Scope**: Core identity, user management, and authentication boundary for the Basket Club Manager (BCM) platform.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement / Constraint | Status | Notes |
| :--- | :--- | :--- | :--- |
| **I. Hexagonal Architecture & Clean Separation** | Domain core isolated; Ports & Adapters; MapStruct mappers between layers. | **PASS** | Domain model `User` contains pure Java 21 logic and `UserStatus` enum. Inbound and outbound ports defined in application layer. Adapters isolate web, security, and persistence. |
| **II. API Design & Documentation-First** | RESTful HTTP APIs documented via OpenAPI 3; RFC 7807 problem details; validation at boundaries. | **PASS** | `OpenApiConfig` and `@Operation` annotations document all endpoints; `ProblemDetail` used for 400, 401, 403, 404, 409, 422, 429, 500 errors; Jakarta validation on request DTOs. |
| **III. Data Persistence, Schema Evolution & Caching** | PostgreSQL transactional store; Flyway versioned migrations; `ddl-auto: none`. | **PASS** | Flyway migrations manage `users` table (`V1__create_users_table.sql` and `V2__add_user_status.sql` or integrated column with default `'ACTIVE'`); `ddl-auto: none` preserved; JPA entities restricted to persistence adapter. |
| **IV. Stateless Security & JWT Authentication** | Stateless JWT bearer tokens; BCrypt hashing; externalized secrets; no server sessions. | **PASS** | `SessionCreationPolicy.STATELESS` configured; JJWT 0.12.6 with `JWT_SECRET`; `BCryptPasswordEncoder(12)` used; self-service ID ownership checks prevent unauthorized mutations; generic 401 prevents enumeration. |
| **V. Automated Testing & Quality Verification** | Unit, slice, and integration tests across layers. | **PASS** | Unit tests for domain/use cases (no Spring context); `@WebMvcTest` for controller boundaries and error handlers; `@DataJpaTest` and security tests for authorization and rate-limiting. |
| **Local Reproducibility Gate** | Dev environment startable with `docker compose up -d`. | **PASS** | PostgreSQL 16 container provided via root `docker-compose.yml`. |

## Project Structure

### Documentation (this feature)

```text
specs/002-users-endpoints-contracts/
├── plan.md              # This implementation plan
├── research.md          # Phase 0 architectural & security research decisions
├── data-model.md        # Phase 1 database schema, entity mapping, and validation contracts
├── quickstart.md        # Phase 1 end-to-end cURL scenarios & test execution guide
├── contracts/           # Phase 1 interface contracts
│   ├── auth-contract.md     # Public registration and login contract (/api/v1/auth)
│   ├── users-contract.md    # Protected user profile management contract (/api/v1/users)
│   └── openapi.yaml         # Complete OpenAPI 3.0 specification
└── tasks.md             # Phase 2 output (generated by /speckit-tasks)
```

### Source Code (repository root)

```text
bcmServer/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── basketapp/
│   │   │           └── bcmserver/
│   │   │               ├── BcmServerApplication.java
│   │   │               ├── domain/
│   │   │               │   ├── exception/
│   │   │               │   │   ├── DomainException.java
│   │   │               │   │   ├── UserAlreadyExistsException.java
│   │   │               │   │   ├── UserNotFoundException.java
│   │   │               │   │   ├── InvalidCredentialsException.java
│   │   │               │   │   ├── AccountStatusException.java
│   │   │               │   │   ├── SemanticValidationException.java
│   │   │               │   │   ├── RateLimitExceededException.java
│   │   │               │   │   └── UnauthorizedOperationException.java
│   │   │               │   └── model/
│   │   │               │       ├── User.java
│   │   │               │       └── UserStatus.java
│   │   │               ├── application/
│   │   │               │   ├── port/
│   │   │               │   │   ├── in/
│   │   │               │   │   │   ├── RegisterUserUseCase.java
│   │   │               │   │   │   ├── LoginUserUseCase.java
│   │   │               │   │   │   ├── GetUserProfileUseCase.java
│   │   │               │   │   │   ├── UpdateUserProfileUseCase.java
│   │   │               │   │   │   ├── DeleteUserUseCase.java
│   │   │               │   │   │   ├── RegisterUserCommand.java
│   │   │               │   │   │   ├── LoginUserCommand.java
│   │   │               │   │   │   ├── UpdateUserCommand.java
│   │   │               │   │   │   └── AuthToken.java
│   │   │               │   │   └── out/
│   │   │               │   │       ├── UserRepositoryPort.java
│   │   │               │   │       ├── PasswordEncoderPort.java
│   │   │               │   │       ├── TokenProviderPort.java
│   │   │               │   │       └── RateLimiterPort.java
│   │   │               │   └── service/
│   │   │               │       ├── UserService.java
│   │   │               │       └── AuthService.java
│   │   │               └── infrastructure/
│   │   │                   ├── adapter/
│   │   │                   │   ├── in/
│   │   │                   │   │   └── web/
│   │   │                   │   │       ├── AuthController.java
│   │   │                   │   │       ├── UserController.java
│   │   │                   │   │       ├── dto/
│   │   │                   │   │       │   ├── RegisterRequest.java
│   │   │                   │   │       │   ├── LoginRequest.java
│   │   │                   │   │       │   ├── UpdateUserRequest.java
│   │   │                   │   │       │   ├── UserProfileResponse.java
│   │   │                   │   │       │   └── AuthResponse.java
│   │   │                   │   │       ├── mapper/
│   │   │                   │   │       │   └── UserWebMapper.java
│   │   │                   │   │       └── exception/
│   │   │                   │   │           └── GlobalExceptionHandler.java
│   │   │                   │   └── out/
│   │   │                   │       ├── persistence/
│   │   │                   │       │   ├── UserJpaEntity.java
│   │   │                   │       │   ├── SpringDataJpaUserRepository.java
│   │   │                   │       │   ├── UserRepositoryAdapter.java
│   │   │                   │       │   └── mapper/
│   │   │                   │       │       └── UserPersistenceMapper.java
│   │   │                   │       ├── security/
│   │   │                   │       │   ├── BCryptPasswordEncoderAdapter.java
│   │   │                   │       │   └── JwtTokenAdapter.java
│   │   │                   │       └── ratelimit/
│   │   │                   │           ├── InMemoryRateLimiterAdapter.java
│   │   │                   │           └── RateLimitingFilter.java
│   │   │                   └── config/
│   │   │                       ├── SecurityConfig.java
│   │   │                       ├── JwtAuthenticationFilter.java
│   │   │                       ├── OpenApiConfig.java
│   │   │                       └── DatabaseConnectionVerifier.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_users_table.sql
│   │               └── V2__add_user_status.sql
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── basketapp/
│       │           └── bcmserver/
│       │               ├── domain/
│       │               │   └── UserTest.java
│       │               ├── application/
│       │               │   ├── UserServiceTest.java
│       │               │   └── AuthServiceTest.java
│       │               └── infrastructure/
│       │                   ├── adapter/
│       │                   │   ├── in/
│       │                   │   │   └── web/
│       │                   │   │       ├── AuthControllerTest.java
│       │                   │   │       ├── UserControllerTest.java
│       │                   │   │       └── GlobalExceptionHandlerTest.java
│       │                   │   └── out/
│       │                   │       ├── persistence/
│       │                   │       │   └── UserRepositoryAdapterTest.java
│       │                   │       ├── security/
│       │                   │       │   ├── JwtTokenAdapterTest.java
│       │                   │       │   └── SecurityConfigTest.java
│       │                   │       └── ratelimit/
│       │                   │           └── RateLimiterAdapterTest.java
│       │                   └── config/
│       └── resources/
│           └── application-test.yml
```

**Structure Decision**: Hexagonal Architecture (Ports and Adapters) layout with base package `com.basketapp.bcmserver`, splitting responsibilities strictly into `domain` (pure POJOs, status enum, business validation, exceptions), `application` (`port/in`, `port/out`, `service`), and `infrastructure` (`adapter/in/web`, `adapter/out/persistence`, `adapter/out/security`, `adapter/out/ratelimit`, `config`).

## Complexity Tracking

> **No violations**: All architectural patterns and technologies align 100% with Constitution Principles I through V.
