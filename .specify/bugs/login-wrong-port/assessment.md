# Bug Assessment: Login Endpoint Connection Error (Socket Hang Up)

- **Slug**: login-wrong-port
- **Created**: 2026-10-30
- **Source**: pasted text
- **Verdict**: invalid
- **Severity**: low

## Report (verbatim or summarized)

```
# login endpoint error connection
* Spec: `002-users-endpoints-contracts`
* Error track
POST http://localhost:5432/api/v1/auth/login
Error: socket hang up
Request Headers
Content-Type: application/json
User-Agent: PostmanRuntime/7.51.1
Accept: */*
Cache-Control: no-cache
Postman-Token: 62bdcaaf-825d-4958-88a8-217fe6c5e1b9
Host: localhost:5432
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```

## Symptom

When sending a `POST` request to `http://localhost:5432/api/v1/auth/login` via Postman, the connection fails with `Error: socket hang up`. The expected behavior is an HTTP response (such as `200 OK`, `400 Bad Request`, or `401 Unauthorized`) from the `AuthController` login endpoint.

## Reproduction

1. Start the PostgreSQL container (`docker compose up -d postgres`) listening on default port `5432`.
2. Send an HTTP `POST` request using Postman or cURL to `http://localhost:5432/api/v1/auth/login`.
3. Observe that PostgreSQL receives non-protocol plaintext HTTP traffic on port `5432` and immediately drops/terminates the TCP connection, causing Postman to report `Error: socket hang up`.

## Suspected Code Paths

- `src/main/resources/application.yml:1-2` — Configures the Spring Boot HTTP server port to `${SERVER_PORT:8080}`.
- `src/main/resources/application.yml:7-8` — Configures datasource connection pointing to PostgreSQL on port `5432` (`jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bcm_db}`).
- `src/main/java/com/basketapp/bcmserver/infrastructure/adapter/in/web/AuthController.java:71-86` — Exposes `/api/v1/auth/login` on the Spring Boot web container (port `8080`).
- `specs/002-users-endpoints-contracts/quickstart.md:39, 99-100` — Documents application server port `8080` for health check and login requests.

## Root Cause Hypothesis

The client (Postman) directed the HTTP request to port `5432` (`localhost:5432`), which is the PostgreSQL database server port rather than the Spring Boot application server port (`8080`). Because PostgreSQL expects PostgreSQL wire protocol communication rather than HTTP requests, it closes the socket abruptly, resulting in `socket hang up`. Confidence: high.

## Proposed Remediation

**Preferred**: Update the Postman request URL / environment variable from port `5432` to port `8080`:
```http
POST http://localhost:8080/api/v1/auth/login
```
Ensure the Spring Boot backend service is running (`./mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`).

**Alternatives** (optional):
- If the application is intended to run on a custom port, set the `SERVER_PORT` environment variable before starting the Spring Boot server (e.g., `SERVER_PORT=8081`), ensuring it does not collide with the PostgreSQL service port `5432`.

**Files likely to change**:
- None in the backend application codebase. Only client configuration (Postman collection or environment).

**Tests to add or update**:
- None needed. Existing web layer tests (`AuthControllerTest`) and integration tests already validate `/api/v1/auth/login` against the web server context.

## Risks & Considerations

- Ensure client environment variables in Postman differentiate between `db_port` (5432) and `server_port` / `api_url` (http://localhost:8080).

## Open Questions

- None.

