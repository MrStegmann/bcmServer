# Environment Variable Contract: Setup BCMServer Project

**Feature**: `001-setup-bcmserver-project`  
**Date**: 2026-10-29  
**Status**: Completed  

---

## 1. Specification

This contract specifies the environment variables read by `bcmServer` during bootstrap to configure the runtime profile and the PostgreSQL datasource.

All sensitive credentials MUST be passed through the environment or a local non-committed `.env` file (based on `.env.example`).

---

## 2. Environment Variables

| Variable Name | Required | Default Value | Description | Example Value |
| :--- | :--- | :--- | :--- | :--- |
| `SERVER_PORT` | No | `8080` | HTTP port on which the Spring Boot server listens. | `8080` |
| `SPRING_PROFILES_ACTIVE` | No | `default` | Comma-separated active Spring profiles. | `dev`, `prod`, `test` |
| `DB_HOST` | No | `localhost` | PostgreSQL host name or container network alias. | `localhost` or `postgres` |
| `DB_PORT` | No | `5432` | PostgreSQL network port. | `5432` |
| `DB_NAME` | No | `bcm_db` | Target PostgreSQL database name. | `bcm_db` |
| `DB_USERNAME` | No | `postgres` | PostgreSQL username. | `postgres` |
| `DB_PASSWORD` | No | `postgres` | PostgreSQL password. | `secret_password` |
| `DB_URL` | No | `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}` | Full JDBC URL override. If set, takes precedence over individual host/port/name variables. | `jdbc:postgresql://db.prod.internal:5432/bcm_db` |

---

## 3. Template File Format (`.env.example`)

The repository root MUST contain `.env.example` with the following content:

```dotenv
# ==========================================
# bcmServer Environment Configuration Template
# ==========================================

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bcm_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Optional: Full JDBC URL Override (if set, overrides DB_HOST, DB_PORT, DB_NAME)
# DB_URL=jdbc:postgresql://localhost:5432/bcm_db
```

---

## 4. Validation & Error Behavior

1. **Missing Variables**: If neither `DB_URL` nor the discrete parameters (`DB_HOST`, `DB_PORT`, `DB_NAME`) can be resolved, Spring Boot application context bootstrap fails with an unresolvable property exception.
2. **Malformed URL**: If `DB_URL` does not conform to JDBC syntax (`jdbc:postgresql://<host>:<port>/<db>`), HikariCP fails immediately with a fatal configuration exception.
3. **Invalid Credentials**: If `DB_USERNAME` or `DB_PASSWORD` is rejected by PostgreSQL, authentication exception (`PSQLException: FATAL: password authentication failed`) is logged, and the application terminates with a non-zero exit code.

