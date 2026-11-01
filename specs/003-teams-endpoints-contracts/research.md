# Research: Team Contracts and Endpoints

**Feature**: `003-teams-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-30  

---

## 1. Context & Architectural Overview

The goal is to implement private CRUD REST API endpoints for the `teams` resource adhering strictly to the project Constitution (Hexagonal Architecture / Ports and Adapters, Spring Boot 3.4.2, Java 21, Flyway migration for PostgreSQL, MapStruct, stateless JWT Spring Security, and Springdoc OpenAPI 2.8.4).

All endpoints must be protected by JWT Bearer authentication, and authentication MUST strictly precede request body and parameter validation.

---

## 2. Research Items & Decisions

### Decision 1: API Route Hierarchy and Versioning
- **Decision**: Expose endpoints under `/api/v1/teams` with alias support for `/teams` if necessary, and document `/api/v1/teams` as the canonical API contract in OpenAPI.
- **Rationale**:
  - The existing codebase uses `/api/v1/users` and `/api/v1/auth` (see `UserController` and `AuthController`).
  - Standardizing on `/api/v1/teams` ensures consistency across all endpoints in `bcmServer`.
  - Spring Web `@RequestMapping({"/api/v1/teams", "/teams"})` can be configured on the controller so both canonical `/api/v1/teams` and the specification's shorthand `/teams` route work transparently without breaking any client expectations.
- **Alternatives Considered**:
  - *Only `/teams`*: Inconsistent with `/api/v1/auth` and `/api/v1/users`, violating API consistency principles in Constitution Principle II.
  - *Only `/api/v1/teams`*: Ideal for long-term consistency; adding `/teams` as alias satisfies the literal spec references (`POST /teams`, `GET /teams`, etc.).

---

### Decision 2: Enforcement of Token Validation Precedence over Data Validation
- **Decision**: Leverage Spring Security's servlet filter architecture (`JwtAuthenticationFilter` placed before `UsernamePasswordAuthenticationFilter`).
- **Rationale**:
  - The specification (FR-003, FR-004, SC-001, Edge Cases) mandates that missing or invalid JWT tokens MUST return `401 Unauthorized` before any payload format or route parameter validation occurs, with zero database interaction.
  - In the Servlet container and Spring Web lifecycle, servlet filters run before the `DispatcherServlet` handler mapping, message converters, and controller method parameter binding/validation (`@Valid`).
  - When a request lacks a valid token or has an expired token, `JwtAuthenticationFilter` does not set the `SecurityContextHolder` authentication, and `SecurityConfig`'s `AuthenticationEntryPoint` immediately writes an RFC 7807 `401 Unauthorized` `ProblemDetail` to the response stream and terminates filter chain execution.
  - This natively guarantees 100% token validation precedence before Jackson parses the JSON or `MethodArgumentNotValidException` can trigger.
- **Alternatives Considered**:
  - *Controller-level token checks*: Manually validating tokens inside controller methods would violate separation of concerns, fail to leverage Spring Security, and allow payload deserialization to execute prior to auth verification.

---

### Decision 3: Hexagonal Architecture Package & Class Structure
- **Decision**: Implement domain models, ports, services, and adapters strictly following the existing `bcmserver` ring structure.
- **Structure**:
  1. **Domain Core** (`com.basketapp.bcmserver.domain`):
     - `model.Team`: Pure Java 21 class/record (id, name, category, createdAt). Domain validation rules (non-blank name, trimmed whitespace, invariants).
     - `exception.TeamNotFoundException`: Extends `DomainException` to signal 404 conditions.
  2. **Application Ports & Services** (`com.basketapp.bcmserver.application`):
     - `port.in`: `CreateTeamUseCase`, `GetTeamUseCase`, `UpdateTeamUseCase`, `DeleteTeamUseCase` (with commands: `CreateTeamCommand`, `UpdateTeamCommand`).
     - `port.out`: `TeamRepositoryPort` (methods: `save`, `findById`, `findAll`, `deleteById`, `existsById`).
     - `service`: `TeamService` implementing all inbound ports and orchestrating domain logic and outbound repository port.
  3. **Inbound Web Adapter** (`com.basketapp.bcmserver.infrastructure.adapter.in.web`):
     - `TeamController`: REST controller handling `/api/v1/teams` and `/teams`.
     - `dto`: `CreateTeamRequest`, `UpdateTeamRequest`, `TeamResponse`.
     - `mapper.TeamWebMapper`: MapStruct mapper between domain and web DTOs.
     - `GlobalExceptionHandler` update: Map `TeamNotFoundException` to HTTP 404 ProblemDetail, handle `MethodArgumentTypeMismatchException` for invalid ID parameters to HTTP 400 ProblemDetail.
  4. **Outbound Persistence Adapter** (`com.basketapp.bcmserver.infrastructure.adapter.out.persistence`):
     - `entity.TeamJpaEntity`: JPA `@Entity` mapped to `teams` table.
     - `repository.SpringDataTeamRepository`: `JpaRepository<TeamJpaEntity, Long>`.
     - `adapter.TeamRepositoryAdapter`: Implements `TeamRepositoryPort`.
     - `mapper.TeamPersistenceMapper`: MapStruct mapper between domain `Team` and `TeamJpaEntity`.
- **Rationale**:
  - Completely isolates persistence entities and annotations (`jakarta.persistence.*`, `@Entity`) from the domain core, strictly honoring Constitution Principle I.
  - All conversions occur at layer boundaries using compile-time MapStruct mappers.
- **Alternatives Considered**:
  - *Direct JPA entities in services/controllers*: Violates Constitution Principle I and Hexagonal Architecture rules.

---

### Decision 4: Flyway Migration for `teams` Table
- **Decision**: Add `src/main/resources/db/migration/V2__create_teams_table.sql`.
- **Schema Mapping**:
  ```sql
  CREATE TABLE teams (
      id BIGSERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      category TEXT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX idx_teams_name ON teams (name);
  ```
- **Rationale**:
  - Matches `docs/data_models.md` exactly: `id SERIAL/BIGSERIAL PRIMARY KEY`, `name TEXT NOT NULL`, `category TEXT NULLABLE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
  - Conforms to Constitution Principle III: Schema evolution managed exclusively via versioned Flyway migrations.
- **Alternatives Considered**:
  - *Hibernate auto DDL*: Prohibited by Constitution (`hibernate.ddl-auto: none` / `validate`).

---

### Decision 5: Request Validation & Path Variable Sanitization
- **Decision**:
  - In `CreateTeamRequest`: `@NotBlank(message = "Team name is required and cannot be blank") @Size(max = 100, message = "Team name must not exceed 100 characters") String name`, `@Size(max = 50, message = "Category must not exceed 50 characters") String category`.
  - In `UpdateTeamRequest`: `@NotBlank(message = "Team name cannot be blank if provided") @Size(max = 100) String name`, `@Size(max = 50) String category`.
  - In path variables: `@PathVariable @Positive(message = "Team ID must be a positive number") Long id`.
  - In `GlobalExceptionHandler`: add handling for `MethodArgumentTypeMismatchException` (e.g. non-numeric `/teams/abc` -> 400 Bad Request) and `ConstraintViolationException` (e.g. negative ID `/teams/-1` -> 400 Bad Request).
- **Rationale**:
  - Guarantees 100% pre-database boundary validation (FR-005, FR-006, Edge Cases).
- **Alternatives Considered**:
  - *Validation inside domain service only*: Violates boundary fail-fast rule and produces 500 or unchecked exceptions instead of clear RFC 7807 400 Bad Request.

---

### Decision 6: HTTP Status Codes & REST Verb Conventions
- **Decision**:
  - `POST /api/v1/teams`: 201 Created with `Location` header (`/api/v1/teams/{id}`) and `TeamResponse` body.
  - `GET /api/v1/teams`: 200 OK with `List<TeamResponse>`.
  - `GET /api/v1/teams/{id}`: 200 OK with `TeamResponse` or 404 Not Found.
  - `PUT /api/v1/teams/{id}`: 200 OK with updated `TeamResponse` or 404 Not Found.
  - `DELETE /api/v1/teams/{id}`: 204 No Content or 404 Not Found.
- **Rationale**:
  - Fully aligns with REST standards, Constitution Principle II, and FR-001, FR-010, FR-011, FR-012, FR-013, FR-014.
- **Alternatives Considered**:
  - *Returning 200 OK for POST*: Deprecated REST practice; 201 Created is the correct standard.
  - *Returning 200 OK with null for DELETE*: 204 No Content is cleaner and adheres to REST semantics.

