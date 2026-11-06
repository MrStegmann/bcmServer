# bcmServer - Architecture & Design Specification

This document provides a comprehensive overview of the software architecture, design patterns, and engineering principles implemented in **bcmServer** (Basket Club Manager Server).

---

## 1. Architectural Philosophy: Hexagonal (Ports & Adapters)

**bcmServer** adheres to **Hexagonal Architecture** (also known as *Ports and Adapters*), formulated by Alistair Cockburn. The primary objective is to decouple core business logic from frameworks, persistence stores, transport layers, and third-party libraries.

### Core Hexagon Diagram

```mermaid
flowchart TD
    subgraph Driving_Adapters["Inbound Adapters (Driving)"]
        HTTP["REST API Web Controllers<br/>(AuthController, UserController, TeamController,<br/>PlayerController, GameController, PlayerStatsController)"]
        Swagger["Springdoc OpenAPI / Swagger UI"]
        RateFilter["RateLimitingFilter"]
        JwtFilter["JwtAuthenticationFilter"]
    end

    subgraph Inbound_Ports["Application Layer: Inbound Ports"]
        UseCases["Use Case Interfaces & Command Records<br/>(RegisterUserUseCase, CreateGameUseCase,<br/>UpdatePlayerStatsUseCase, etc.)"]
    end

    subgraph Core_Domain["Core Domain Layer (Pure Java 21)"]
        DomainEntities["Domain Entities & Enums<br/>(User, Team, Player, Game, PlayerStats,<br/>UserStatus, GameStatus)"]
        DomainExceptions["Domain Exceptions<br/>(DomainException, SemanticValidationException,<br/>AccountStatusException, etc.)"]
    end

    subgraph Application_Services["Application Services (Orchestrators)"]
        AppServices["Services with Transactional Boundaries<br/>(AuthService, UserService, TeamService,<br/>PlayerService, GameService, PlayerStatsService)"]
    end

    subgraph Outbound_Ports["Application Layer: Outbound Ports"]
        RepoPorts["Repository Ports<br/>(UserRepositoryPort, TeamRepositoryPort,<br/>PlayerRepositoryPort, GameRepositoryPort,<br/>PlayerStatsRepositoryPort)"]
        SecPorts["Security & Utility Ports<br/>(TokenProviderPort, PasswordEncoderPort, RateLimiterPort)"]
    end

    subgraph Driven_Adapters["Outbound Adapters (Driven)"]
        JPA["Spring Data JPA & Hibernate 6<br/>(JpaEntities, SpringDataRepositories,<br/>RepositoryAdapters, PersistenceMappers)"]
        DB[(PostgreSQL 16 / Supabase)]
        SecurityAdapter["JJWT 0.12.x Token Provider<br/>BCrypt Password Encoder"]
        RateLimitAdapter["In-Memory Sliding Window Rate Limiter"]
    end

    %% Wiring
    HTTP --> UseCases
    RateFilter --> HTTP
    JwtFilter --> HTTP
    UseCases --> AppServices
    AppServices --> DomainEntities
    AppServices --> DomainExceptions
    AppServices --> RepoPorts
    AppServices --> SecPorts
    RepoPorts --> JPA
    JPA --> DB
    SecPorts --> SecurityAdapter
    SecPorts --> RateLimitAdapter
```

### Dependency Rule
Dependencies **only point inward**:
- **Domain**: Knows nothing about Spring, JPA, HTTP, or databases.
- **Application**: Knows domain models and defines ports (interfaces). Knows nothing about database schemas, HTTP status codes, or frameworks.
- **Infrastructure (Adapters)**: Implements inbound controllers and outbound repository adapters. It depends on the application layer, never the other way around.

---

## 2. Layer Structure & Responsibilities

```text
com.basketapp.bcmserver/
├── domain/                                  # CORE: Pure Java 21 domain models
│   ├── model/
│   │   ├── User.java                        # User entity & credentials
│   │   ├── UserStatus.java                  # Enum: ACTIVE, INACTIVE, LOCKED
│   │   ├── Team.java                        # Team entity & division
│   │   ├── Player.java                      # Player entity & jersey attributes
│   │   ├── Game.java                        # Game schedule, scores & fouls
│   │   ├── GameStatus.java                  # Enum: PENDING, LIVE, FINISHED
│   │   └── PlayerStats.java                 # Box-score statistics & PIR
│   └── exception/                           # Business domain exceptions
│       ├── DomainException.java             # Base unchecked domain exception
│       ├── AccountStatusException.java      # 403 Forbidden on locked/inactive
│       ├── InvalidCredentialsException.java # 401 Unauthorized
│       ├── RateLimitExceededException.java  # 429 Too Many Requests
│       ├── SemanticValidationException.java # 422 Unprocessable Entity
│       ├── UnauthorizedOperationException.java # 403 Forbidden
│       ├── UserAlreadyExistsException.java  # 409 Conflict
│       ├── UserNotFoundException.java       # 404 Not Found
│       ├── TeamNotFoundException.java       # 404 Not Found
│       ├── PlayerNotFoundException.java     # 404 Not Found
│       ├── GameNotFoundException.java       # 404 Not Found
│       └── PlayerStatsNotFoundException.java# 404 Not Found
│
├── application/                             # APPLICATION LAYER (Ports & Services)
│   ├── port/
│   │   ├── in/                              # Inbound Ports (Use Cases & Commands)
│   │   │   ├── RegisterUserUseCase.java & RegisterUserCommand.java
│   │   │   ├── LoginUserUseCase.java & LoginUserCommand.java
│   │   │   ├── CreateTeamUseCase.java, UpdateTeamUseCase.java, etc.
│   │   │   ├── CreatePlayerUseCase.java, UpdatePlayerUseCase.java, etc.
│   │   │   ├── CreateGameUseCase.java, UpdateGameUseCase.java, etc.
│   │   │   └── CreatePlayerStatsUseCase.java, UpdatePlayerStatsUseCase.java, etc.
│   │   └── out/                             # Outbound Ports (Driven Interfaces)
│   │       ├── UserRepositoryPort.java
│   │       ├── TeamRepositoryPort.java
│   │       ├── PlayerRepositoryPort.java
│   │       ├── GameRepositoryPort.java
│   │       ├── PlayerStatsRepositoryPort.java
│   │       ├── TokenProviderPort.java
│   │       ├── PasswordEncoderPort.java
│   │       └── RateLimiterPort.java
│   └── service/                             # Application Services (Use Case Implementation)
│       ├── AuthService.java                 # Login & Registration workflows
│       ├── UserService.java                 # Profile retrieval, update, deletion
│       ├── TeamService.java                 # Team roster operations
│       ├── PlayerService.java               # Player assignment and CRUD
│       ├── GameService.java                 # Schedule and live score updates
│       └── PlayerStatsService.java          # Individual statistics & PIR calculation
│
└── infrastructure/                          # ADAPTERS & INFRASTRUCTURE
    ├── adapter/
    │   ├── in/web/                          # REST Inbound Driving Adapters
    │   │   ├── AuthController.java          # /api/v1/auth
    │   │   ├── UserController.java          # /api/v1/users
    │   │   ├── TeamController.java          # /api/v1/teams
    │   │   ├── PlayerController.java        # /api/v1/players
    │   │   ├── GameController.java          # /api/v1/games
    │   │   ├── PlayerStatsController.java   # /api/v1/player-stats
    │   │   ├── dto/                         # Immutable request/response records
    │   │   ├── mapper/                      # MapStruct web mappers
    │   │   ├── filter/                      # Inbound servlet filters (Rate limiting)
    │   │   └── exception/                   # GlobalExceptionHandler (RFC 7807)
    │   └── out/                             # Outbound Driven Adapters
    │       ├── persistence/                 # JPA Repositories, Entities & Mappers
    │       ├── security/                    # JJWT Token Provider & BCrypt Adapter
    │       └── ratelimit/                   # In-memory sliding-window limiter
    └── config/                              # Spring configurations
        ├── SecurityConfig.java              # Security filter chain & rules
        ├── JwtAuthenticationFilter.java     # Stateless JWT parsing
        ├── OpenApiConfig.java               # Swagger & OpenAPI metadata
        ├── DatabaseConnectionVerifier.java  # Fail-fast startup connectivity check
        └── DotenvEnvironmentPostProcessor.java # .env file resolution
```

---

## 3. Request Flow & Execution Lifecycle

### Scenario A: Authenticated Domain Operation (e.g. Updating Game Score)

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client
    participant SecFilter as JwtAuthenticationFilter
    participant Controller as GameController
    participant Mapper as GameWebMapper
    participant Service as GameService
    participant Domain as Game (Domain Entity)
    participant RepoPort as GameRepositoryPort
    participant RepoAdapter as GameRepositoryAdapter
    participant DB as PostgreSQL 16

    Client->>SecFilter: PUT /api/v1/games/1 (Bearer JWT + JSON)
    Note over SecFilter: 1. Validate JWT signature & expiration<br/>2. Populate SecurityContextHolder
    SecFilter->>Controller: Forward request
    Note over Controller: 3. Bean Validation (@Valid UpdateGameRequest)<br/>4. Map to UpdateGameCommand
    Controller->>Service: updateGame(command)
    Service->>RepoPort: findById(1)
    RepoPort->>RepoAdapter: findById(1)
    RepoAdapter->>DB: SELECT * FROM games WHERE id = 1
    DB-->>RepoAdapter: GameJpaEntity
    RepoAdapter-->>Service: Game domain model
    Service->>Domain: updateScoresAndFouls(...)
    Note over Domain: Enforces invariants:<br/>- Non-negative scores<br/>- Non-negative fouls
    Service->>RepoPort: save(game)
    RepoPort->>RepoAdapter: save(game)
    RepoAdapter->>DB: UPDATE games SET ...
    DB-->>RepoAdapter: OK
    RepoAdapter-->>Service: Updated Game domain model
    Service-->>Controller: Game
    Controller->>Mapper: toResponse(game)
    Mapper-->>Controller: GameResponse
    Controller-->>Client: HTTP 200 OK (GameResponse JSON)
```

### Scenario B: Public Authentication with Rate Limiting (Login Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client
    participant RateLimiter as RateLimitingFilter
    participant Controller as AuthController
    participant Service as AuthService
    participant UserRepo as UserRepositoryPort
    participant Encoder as PasswordEncoderPort
    participant TokenProvider as TokenProviderPort
    participant DB as PostgreSQL 16

    Client->>RateLimiter: POST /api/v1/auth/login
    Note over RateLimiter: Check IP window counter
    alt Rate limit exceeded (> 5 req / 60s)
        RateLimiter-->>Client: HTTP 429 Too Many Requests (Retry-After)
    else Rate limit ok
        RateLimiter->>Controller: Forward request
        Controller->>Service: login(command)
        Service->>UserRepo: findByUsername(username)
        UserRepo->>DB: SELECT * FROM users WHERE username = ?
        DB-->>UserRepo: User record
        alt User not found or password mismatch
            Service-->>Controller: throw InvalidCredentialsException
            Controller-->>Client: HTTP 401 Unauthorized (ProblemDetail)
        else Account status is LOCKED or INACTIVE
            Service-->>Controller: throw AccountStatusException
            Controller-->>Client: HTTP 403 Forbidden (ProblemDetail)
        else Credentials valid & active
            Service->>Encoder: matches(rawPassword, passwordHash)
            Service->>TokenProvider: generateToken(user)
            TokenProvider-->>Service: JWT string
            Service-->>Controller: AuthenticationResult(token, user)
            Controller-->>Client: HTTP 200 OK (AuthResponse)
        end
    end
```

---

## 4. Key Architectural Decisions (ADRs)

### ADR-01: Framework Isolation in the Domain
- **Decision**: Domain entities (`User`, `Team`, `Player`, `Game`, `PlayerStats`) contain no JPA, Hibernate, Jackson, or Spring annotations.
- **Rationale**: Isolates business invariants from framework bugs, library updates, or future migrations (e.g., migrating from JPA to JDBC/R2DBC or switching frameworks).

### ADR-02: Compile-Time Mapping with MapStruct
- **Decision**: Zero reflection-based object mapper (e.g., ModelMapper) is allowed. All translations between Web DTOs, Domain Entities, and JPA Entities are performed by MapStruct compile-time generated mappers.
- **Rationale**: Eliminates runtime reflection overhead, fails fast at compile time if fields are missing or incompatible, and produces zero-overhead bytecode.

### ADR-03: Deterministic Schema Management with Flyway
- **Decision**: Hibernate DDL auto-generation is disabled (`spring.jpa.hibernate.ddl-auto: none`). All schema modifications are handled by Flyway versioned SQL scripts (`V1` to `V6`).
- **Rationale**: Avoids dangerous DDL locks, unexpected table modifications in production, and provides an auditable history of the database schema.

### ADR-04: Startup Database Fail-Fast Verification
- **Decision**: A dedicated `DatabaseConnectionVerifier` runs at `Order(1)` upon application startup.
- **Rationale**: If PostgreSQL or Supabase is unreachable, misconfigured, or has invalid credentials, the server aborts bootstrapping immediately (< 10 seconds), preventing the deployment of broken instances.

### ADR-05: Standardized RFC 7807 Problem Details
- **Decision**: All REST error responses conform to `application/problem+json` (RFC 7807) using Spring's `ProblemDetail`.
- **Rationale**: Provides consistent, machine-readable error responses across all endpoints with structured fields (`title`, `status`, `detail`, `instance`, and optional `invalidParams`).

