# Research & Architecture Decisions: Setup BCMServer Project

**Feature**: `001-setup-bcmserver-project`  
**Date**: 2026-10-29  
**Status**: Completed  

---

## 1. Project Build System & Framework Baseline

### Decision
Use **Maven 3.9+** with **Java 21** as compiler target and **Spring Boot 3.4.x** (latest stable Spring Boot 3 release) as parent starter. Provide the Maven Wrapper (`mvnw` / `mvnw.cmd`) in the project root.

### Rationale
- **Constitution Alignment**: The bcmServer Constitution specifically mandates Java 21 and Spring Boot 3.x.
- **Java 21 Features**: LTS release supporting virtual threads, pattern matching, record patterns, and modern language constructs.
- **Maven Wrapper**: Guarantees zero-drift builds across local developer machines and CI/CD pipelines without requiring pre-installed Maven binaries.

### Alternatives Considered
- **Gradle**: Rejected because the user specification explicitly mandates Maven.
- **Java 17**: Rejected because Java 21 is mandated by the Constitution and offers enhanced virtual thread support and performance optimizations.
- **Spring Boot 2.x**: Rejected because Spring Boot 3 is required for Java 21 baseline and Jakarta EE 10 namespace.

---

## 2. Hexagonal Architecture Package Layout

### Decision
Establish the base package `com.basketapp.bcmserver` with strict hexagonal boundaries:
```text
src/main/java/com/basketapp/bcmserver/
├── BcmServerApplication.java
├── domain/                      # Pure Java: models, exceptions, domain services (zero Spring/JPA deps)
├── application/                 # Use cases and port contracts
│   ├── port/
│   │   ├── in/                  # Inbound ports (use case interfaces)
│   │   └── out/                 # Outbound ports (repository/client SPIs)
│   └── service/                 # Use case implementations
└── infrastructure/              # Adapters and technical drivers
    ├── adapter/
    │   ├── in/                  # Driving adapters
    │   │   └── web/             # REST controllers, request/response DTOs
    │   └── out/                 # Driven adapters
    │       └── persistence/     # Spring Data JPA repositories, JPA entities, mappers
    └── config/                  # Spring configurations, DataSource, OpenAPI, Security beans
```

### Rationale
- **Constitution Alignment**: Principle I (Hexagonal Architecture & Clean Separation) mandates strict boundary separation. Setting up this structure during bootstrap guarantees future features slot into their respective rings without refactoring.
- **Future Scalability**: Inbound/outbound ports decouple business logic completely from framework dependencies.

### Alternatives Considered
- **Layered by Technical Type** (`controller`, `service`, `repository`, `model`): Rejected because it mixes domain logic with infrastructure annotations and violates Constitution Principle I.
- **Flat Package**: Rejected as unmaintainable for enterprise Spring Boot services.

---

## 3. Database Connection & Connection Pool Configuration

### Decision
- Use `spring-boot-starter-data-jpa` backed by `org.postgresql:postgresql` driver.
- Configure HikariCP as the connection pool with explicit connection timeout (`connection-timeout: 10000` = 10s) and validation query behavior.
- Configure Hibernate `ddl-auto: none` (passive mode) to strictly prohibit automatic DDL execution against PostgreSQL.
- Provide a startup connectivity verifier (`DatabaseConnectionVerifier`) via `ApplicationRunner` or Actuator health checking that validates connectivity on bootstrap and logs clear diagnostic details.

### Rationale
- **Constitution Alignment**: Principle III mandates relational data safety and passive `ddl-auto` (`none` or `validate`). Flyway will handle migrations in subsequent features.
- **Fail-Fast Requirement**: Edge cases in the spec require the application to fail fast within 10 seconds if PostgreSQL is unreachable rather than hanging or blocking indefinitely. HikariCP `connectionTimeout: 10000ms` guarantees this behavior.
- **Validation**: Testing `DataSource.getConnection().isValid(2)` provides deterministic startup verification without mutating or querying business tables.

### Alternatives Considered
- **Hibernate `ddl-auto: update` or `create-drop`**: Strictly prohibited by the Constitution and Requirement FR-006.
- **Manual JDBC connection without connection pool**: Rejected because HikariCP is production-grade, default in Spring Boot, and handles pool management and health checks efficiently.

---

## 4. Environment-Based Configuration & Secret Management

### Decision
Externalize all connection parameters in `src/main/resources/application.yml` using standard environment variable placeholders with sensible local development defaults:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bcm_db}}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-timeout: 10000
      maximum-pool-size: 10
      minimum-idle: 2
      pool-name: BcmHikariPool
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
    show-sql: false
```
Provide `.env.example` in the project root documenting required variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`

### Rationale
- **Constitution Alignment**: Principle IV mandates that credentials and secrets MUST NEVER be committed to source code and MUST be provided through externalized configuration.
- **12-Factor App Compliance**: Facilitates seamless deployment across local workstations, Docker containers, and CI pipelines without code changes.

### Alternatives Considered
- **Hardcoding credentials in `application-dev.yml`**: Rejected due to risk of credential leakage and violation of FR-004 and Constitution Principle IV.
- **Third-party secret vault (HashiCorp Vault / AWS Secrets Manager) in baseline setup**: Over-engineering for this setup phase; environment variable interpolation is standard and compatible with all secret injection mechanisms.

---

## 5. Local Infrastructure Orchestration (Docker Compose)

### Decision
Provide `docker-compose.yml` in the project root defining a `postgres` service matching the PostgreSQL requirements:
- Image: `postgres:16-alpine`
- Environment variables: `POSTGRES_DB=bcm_db`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`
- Port binding: `5432:5432`
- Healthcheck: `pg_isready -U postgres -d bcm_db`
- Volume: `postgres_data` persistent volume

### Rationale
- **Constitution Alignment**: Development Workflow section states: "The local development environment MUST be startable with a standard `docker compose up -d` workflow providing required backing services (PostgreSQL, Redis)."
- **Reproducibility**: Developers can spin up a dedicated database in seconds with a single command.

### Alternatives Considered
- **Embedded H2 Database**: Rejected because PostgreSQL specific syntax, types, and connections are required; using H2 produces environment parity mismatch.
- **Requiring pre-installed bare-metal PostgreSQL**: Impedes developer onboarding and risks port/version conflicts.

---

## 6. Testing Strategy & Startup Verification

### Decision
Implement:
1. `BcmServerApplicationTests` using `@SpringBootTest` to verify that the application context loads successfully.
2. An isolated integration test using `@DataJpaTest` / Testcontainers for PostgreSQL or profile-driven configuration to verify actual PostgreSQL driver negotiation and pool creation.
3. Edge case tests to verify fail-fast behavior when the database is unreachable or credentials are missing/invalid.

### Rationale
- **Constitution Alignment**: Principle V (Automated Testing & Quality Verification) requires automated test verification across layers.
- **Spec Verification**: Directly satisfies Acceptance Scenarios for Stories 1, 2, and 3.

### Alternatives Considered
- **Manual-only testing**: Prohibited by Constitution Principle V.

