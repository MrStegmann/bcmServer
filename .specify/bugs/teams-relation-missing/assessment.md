# Bug Assessment: Unexpected error during transaction (relation "teams" does not exist)

- **Slug**: teams-relation-missing
- **Created**: 2026-10-31T09:54:00+01:00
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

```text
# Unexpected error during transaction
## When happens
When the system take a request (GET, POST) for `/teams` endpoints, with a valid JWT token and valid body (for POST), it returns a "Internal Server Error" with status `500`.
This happens with any endpoint in teams.
## Server Terminal Log
2026-10-31T09:48:00.822+01:00  WARN 8164 --- [bcmServer] [nio-8080-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 42P01
2026-10-31T09:48:00.822+01:00 ERROR 8164 --- [bcmServer] [nio-8080-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : ERROR: relation "teams" does not exist
  Position: 13
## Postman Error Response
{
    "type": "about:blank",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "An unexpected error occurred",
    "instance": "/api/v1/teams"
}
```

## Symptom

When sending an authenticated HTTP request (`GET`, `POST`, `PUT`, `DELETE`) to `/teams` or `/api/v1/teams`, the server responds with HTTP `500 Internal Server Error` instead of processing the transaction. In the server logs, Hibernate/JDBC throws SQLState `42P01`: `ERROR: relation "teams" does not exist`.

## Reproduction

1. Start PostgreSQL via Docker Compose (`docker compose up -d`).
2. Start the server without passing an active profile (`./mvnw spring-boot:run` or default profile).
3. Obtain a valid JWT bearer token via `POST /api/v1/auth/login`.
4. Send `GET /api/v1/teams` or `POST /api/v1/teams` with `Authorization: Bearer <TOKEN>` and valid payload `{"name":"Cadete A"}`.
5. The request fails with HTTP 500 and the log displays `ERROR: relation "teams" does not exist`.

## Suspected Code Paths

- `src/main/resources/application.yml:28-32` — `spring.flyway.enabled` is defaulted to `false` via `${SPRING_FLYWAY_ENABLED:false}`. When the application runs without the `dev` profile or an explicit environment variable, Flyway migrations do not execute upon startup.
- `src/main/resources/application.yml:21-22` — `spring.jpa.hibernate.ddl-auto: none` correctly prevents Hibernate from generating schema dynamically, strictly relying on Flyway.
- `src/main/resources/db/migration/V2__create_teams_table.sql` — Migration script exists in the repository, but has never been applied to the live PostgreSQL instance because Flyway migration was skipped.
- `src/main/java/com/basketapp/bcmserver/infrastructure/adapter/out/persistence/TeamJpaEntity.java:18` — `@Table(name = "teams")` expects the `teams` relational table to exist in the connected database schema.
- `src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/exception/GlobalExceptionHandler.java:95-100` — Generic fallback exception handler converts uncaught `BadSqlGrammarException` / `CannotCreateTransactionException` into RFC 7807 500 ProblemDetail.

## Root Cause Hypothesis

In `src/main/resources/application.yml`, `spring.flyway.enabled` is defaulted to `false` (`${SPRING_FLYWAY_ENABLED:false}`). Although `application-dev.yml` enables Flyway with `enabled: true`, starting the server with default configuration (`default` profile) disables Flyway. 

Because `hibernate.ddl-auto` is set to `none` (as mandated by Constitution Principle III), neither Flyway nor Hibernate creates or updates the database schema on startup. Consequently, `V2__create_teams_table.sql` was never executed against `bcm_db` (where `flyway_schema_history` currently only contains `V1__create_users_table.sql`). When any query executes against `teams`, PostgreSQL throws `relation "teams" does not exist` (42P01).

Confidence: **High** (verified via `docker exec bcm-postgres psql -U postgres -d bcm_db -c "\dt"` confirming `teams` table is absent and `flyway_schema_history` has only `V1`).

## Proposed Remediation

**Preferred**:
1. Update `src/main/resources/application.yml` to set `spring.flyway.enabled: ${SPRING_FLYWAY_ENABLED:true}` by default. In a Spring Boot application strictly following Flyway migration practices (Constitution Principle III: *"Schema migrations MUST be managed exclusively via Flyway versioned migration scripts"*), Flyway should be enabled by default so schema migrations run automatically on startup across standard local and runtime environments unless explicitly disabled.
2. Ensure that starting the application or running Flyway applies `V2__create_teams_table.sql` to create `teams` and `idx_teams_name` in PostgreSQL.

**Alternatives**:
- *Require running with `dev` profile*: Pass `-Dspring-boot.run.profiles=dev` or set `SPRING_PROFILES_ACTIVE=dev`. Trade-off: Fragile developer experience; default execution (`./mvnw spring-boot:run` without arguments) continues to fail unexpectedly with 500 errors.
- *Manually execute Flyway migration via CLI or Maven plugin*: Run `mvn flyway:migrate`. Trade-off: Adds extra manual friction outside the standard application lifecycle.

**Files likely to change**:
- `src/main/resources/application.yml`

**Tests to add or update**:
- Add integration test or verification in `EnvironmentConfigurationTest` or a dedicated Flyway migration test to ensure `spring.flyway.enabled` defaults to `true` and Flyway migrations execute successfully against the database.

## Risks & Considerations

- **Existing environments**: Changing the default to `true` means any running instance will automatically apply pending Flyway migrations on startup. Because `baseline-on-migrate: true` and `validate-on-migrate: true` are already set, Flyway safely checks checksums and applies only unapplied scripts (`V2`).
- **Test environments**: Ensure slice tests (`@WebMvcTest`, `@DataJpaTest`) that disable Flyway or use H2/mocks continue to function without issues.

## Open Questions

- None.

