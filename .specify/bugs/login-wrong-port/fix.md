# Bug Fix: Login Endpoint Connection Error (Socket Hang Up)

- **Slug**: login-wrong-port
- **Fixed**: 2026-10-30
- **Assessment**: ./assessment.md
- **Status**: not-applied

## Summary

No code modifications were applied because the bug assessment determined the verdict is `invalid`. The error was caused by a client-side configuration targeting the PostgreSQL database port (5432) instead of the Spring Boot application server port (8080).

## Changes

| File | Change | Notes |
|------|--------|-------|
| _None_ | N/A | No codebase changes required. |

## Tests Added or Updated

- _None_ — Existing tests already validate `AuthController` and the `/api/v1/auth/login` endpoint on the application server.

## Local Verification

- Commands run: Inspected `assessment.md` verdict (`invalid`) and confirmed application configuration in `src/main/resources/application.yml` exposes server on port 8080 (`SERVER_PORT:8080`) while database operates on port 5432.
- Manual checks: Verified that client requests should target `http://localhost:8080/api/v1/auth/login`.

## Deviations from Assessment

None. The assessment identified this as an invalid bug requiring only client-side URL correction in Postman (`http://localhost:8080/api/v1/auth/login`).

## Follow-ups

- Update Postman request collection or environment variable to use `http://localhost:8080` (or `http://localhost:${SERVER_PORT:8080}`).

