<!--
Sync Impact Report:
- Version change: Template → 1.0.0
- Ratification Date: 2026-10-27
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Hexagonal Architecture & Clean Separation (Ports and Adapters)
  - [PRINCIPLE_2_NAME] → II. API Design & Documentation-First (Spring Web & OpenAPI)
  - [PRINCIPLE_3_NAME] → III. Data Persistence, Schema Evolution & Caching (PostgreSQL, Flyway, JPA & Redis)
  - [PRINCIPLE_4_NAME] → IV. Stateless Security & JWT Authentication
  - [PRINCIPLE_5_NAME] → V. Automated Testing & Quality Verification
- Added sections:
  - Technology Stack & System Constraints (Java 21, Spring Boot 3, MapStruct, Docker)
  - Development Workflow & Architectural Quality Gates
- Removed sections: None
- Follow-up TODOs: None (all core tokens resolved)
-->

# bcmServer Constitution

## Core Principles

### I. Hexagonal Architecture & Clean Separation (Ports and Adapters)
The application MUST strictly adhere to Hexagonal Architecture (Ports and Adapters).
- **Domain Core**: Pure Java 21 business entities and logic. The domain core MUST have ZERO dependencies on external frameworks, Spring annotations, persistence libraries, or web packages.
- **Application Layer**: Inbound and outbound ports (interfaces) defining use cases and external interactions.
- **Infrastructure Layer (Adapters)**: Inbound adapters (e.g., Spring Web REST Controllers) drive the application; Outbound adapters (e.g., Spring Data JPA repositories, Redis clients, external services) implement outbound ports.
- **Model Isolation & Mapping**: DTOs, domain models, and JPA `@Entity` classes MUST remain completely separated. Cross-layer conversions MUST be performed via MapStruct mappers. Persistence entities MUST NEVER leak into the domain core or web responses.

### II. API Design & Documentation-First (Spring Web & OpenAPI)
All external interfaces MUST be exposed via RESTful HTTP APIs built with Spring Web MVC and documented comprehensively.
- **Contract Clarity**: Every public endpoint MUST be documented using Springdoc OpenAPI (Swagger) annotations with explicit descriptions, request bodies, response models, and error responses (RFC 7807 problem details).
- **Uniform Semantics**: HTTP verbs (GET, POST, PUT, PATCH, DELETE) and standard HTTP status codes MUST be applied strictly according to REST standards.
- **Input Validation**: All incoming requests MUST be validated at the boundary using `jakarta.validation` annotations prior to reaching application use cases.

### III. Data Persistence, Schema Evolution & Caching (PostgreSQL, Flyway, JPA & Redis)
Relational data and operational cache MUST be managed with deterministic safety and consistency.
- **Relational Integrity**: PostgreSQL is the primary transactional store. Schema migrations MUST be managed exclusively via Flyway versioned migration scripts (`V<version>__<description>.sql`). Hibernate `ddl-auto` MUST be set to `validate` or `none` in all environments.
- **JPA & ORM Encapsulation**: Spring Data JPA and Hibernate MUST remain contained inside persistence adapters. Queries MUST be optimized to avoid N+1 issues using explicit fetch joins, entity graphs, or projections.
- **Distributed Caching**: Redis (via Spring Data Redis) MUST be utilized for caching transient, read-heavy data and session/token management where required. All cache entries MUST define explicit Time-To-Live (TTL) and eviction policies to prevent stale state and memory exhaustion.

### IV. Stateless Security & JWT Authentication
Authentication and authorization MUST be centralized, declarative, and stateless.
- **Stateless Auth**: Authentication MUST use Spring Security configured for stateless JWT (JSON Web Tokens). No server-side HTTP session state is permitted.
- **Granular Authorization**: Endpoint and use-case security MUST enforce role-based and authority-based access control (RBAC) via annotations or security filter chains.
- **Secrets & Configuration**: Secrets, JWT keys, and credentials MUST NEVER be committed to source code; they MUST be provided through environment variables and externalized configuration.

### V. Automated Testing & Quality Verification
Reliability and regression prevention MUST be maintained across all architectural rings.
- **Unit Testing**: Domain logic and application services MUST have comprehensive unit test coverage with fast execution (pure JUnit 5 and Mockito, without loading Spring context).
- **Integration Testing**: Inbound and outbound adapters (REST controllers, JPA repositories, Redis integration) MUST be tested using Spring slice tests (`@WebMvcTest`, `@DataJpaTest`) and full integration tests backed by Docker/Testcontainers for PostgreSQL and Redis.
- **No Untested Use Cases**: Every production use case MUST be accompanied by verification tests before merging.

## Technology Stack & System Constraints

- **Language & Runtime**: Java 21 (utilizing modern language features such as records, sealed classes, pattern matching, and virtual threads where beneficial).
- **Framework Core**: Spring Boot 3.x.
- **Web & Documentation**: Spring Web (REST MVC), Springdoc OpenAPI 2.x (Swagger UI & OpenAPI 3).
- **Persistence & Migration**: PostgreSQL, Spring Data JPA / Hibernate, Flyway.
- **Caching & Key-Value**: Redis via Spring Data Redis.
- **Mapping**: MapStruct for performant, compile-time object mapping between layers.
- **Security**: Spring Security with JWT (stateless bearer token verification).
- **Environment & Orchestration**: Docker and Docker Compose for local service dependencies (PostgreSQL, Redis, and application containers) ensuring reproducible environments across development and CI.

## Development Workflow & Architectural Quality Gates

- **Local Reproducibility**: The local development environment MUST be startable with a standard `docker compose up -d` workflow providing required backing services (PostgreSQL, Redis).
- **Architectural Boundary Enforcement**:
  - Pull requests MUST be rejected if infrastructure details (JPA annotations, Spring HTTP annotations) leak into the domain module/package.
  - No direct JPA entity access from controller layer.
  - Flyway migrations MUST be immutable once merged.
- **Code Cleanliness & Standards**: Automated formatting, static analysis, and zero compiler warnings MUST be maintained across the codebase.

## Governance

- **Supremacy**: This Constitution supersedes all informal design decisions, local practices, and ad-hoc conventions.
- **Amendments**: Amendments require explicit justification, documentation of affected architectural patterns, and consensus.
- **Versioning Policy**: Semantic versioning is strictly enforced:
  - MAJOR bump for fundamental architectural modifications or principle deprecations.
  - MINOR bump for introducing new principles, libraries, or expanding stack standards.
  - PATCH bump for typographical fixes, clarifications, or non-functional wording refinements.
- **Compliance Review**: All design specifications, implementation plans, and code reviews MUST be evaluated against this Constitution.

**Version**: 1.0.0 | **Ratified**: 2026-10-27 | **Last Amended**: 2026-10-27
