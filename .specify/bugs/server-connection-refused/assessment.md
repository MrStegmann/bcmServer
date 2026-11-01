# Bug Assessment: Server Connection Refused on Port 8080 (ECONNREFUSED)

- **Slug**: server-connection-refused
- **Created**: 2026-10-30
- **Source**: pasted text
- **Verdict**: invalid
- **Severity**: low

## Report (verbatim or summarized)

```
POST http://localhost:8080/api/v1/auth/login
Error: connect ECONNREFUSED 127.0.0.1:8080
Request Headers
Content-Type: application/json
User-Agent: PostmanRuntime/7.51.1
Accept: */*
Cache-Control: no-cache
Postman-Token: 8baa6e32-b74b-488b-aae3-fa82ec5bb121
Host: localhost:8080
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```

## Symptom

When sending a `POST` request to `http://localhost:8080/api/v1/auth/login` via Postman, the connection fails immediately with `Error: connect ECONNREFUSED 127.0.0.1:8080`. Expected behavior is an HTTP response from the backend service.

## Reproduction

1. Ensure the Spring Boot backend server is stopped (no process listening on TCP port 8080).
2. Send an HTTP `POST` request to `http://localhost:8080/api/v1/auth/login` using Postman or cURL.
3. Observe that the OS network stack immediately rejects the TCP connection because no socket is bound/listening on port 8080, returning `ECONNREFUSED`.

## Suspected Code Paths

- `docker-compose.yml:1-24` — Defines only the `postgres` container service on port 5432; the Spring Boot application container is not configured in Docker Compose.
- `specs/002-users-endpoints-contracts/quickstart.md:27-39` — Specifies that running the application requires executing `.\mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run` (Linux/macOS) in addition to starting PostgreSQL.
- `src/main/resources/application.yml:1-2` — Configures the HTTP server port to `${SERVER_PORT:8080}`.

## Root Cause Hypothesis

The Spring Boot application server is not currently running. Running `docker compose up` only started the PostgreSQL database container (`bcm-postgres` on port `5432`). The backend application (`bcmServer`) runs independently on the host on port `8080` and was not started, resulting in TCP connection refusal (`ECONNREFUSED 127.0.0.1:8080`). Confidence: high.

## Proposed Remediation

**Preferred**: Start the backend Spring Boot application:

1. Confirm PostgreSQL is running:
   ```powershell
   docker compose up -d postgres
   ```
2. Start the Spring Boot server in a terminal:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Verify that the application is running and healthy:
   ```powershell
   curl http://localhost:8080/actuator/health
   ```
4. Re-issue the login request in Postman to `http://localhost:8080/api/v1/auth/login`.

**Alternatives** (optional):
- Containerize the Spring Boot application alongside Postgres by adding an `app` service to `docker-compose.yml` with a multi-stage Dockerfile.

**Files likely to change**:
- None in the application codebase (operational startup step only).

**Tests to add or update**:
- None needed.

## Risks & Considerations

- If starting Spring Boot fails on startup, verify that database credentials in `.env` / `application.yml` match the PostgreSQL container configuration and that Flyway migrations or JPA table setup complete cleanly.

## Open Questions

- None.

