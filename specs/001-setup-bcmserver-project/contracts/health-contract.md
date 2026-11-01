# Health & Startup Verification Contract: Setup BCMServer Project

**Feature**: `001-setup-bcmserver-project`  
**Date**: 2026-10-29  
**Status**: Completed  

---

## 1. Specification Overview

This contract governs the observable interfaces exposed by `bcmServer` to verify application readiness and database connectivity:
1. **HTTP Health Endpoint**: Exposing standard liveness and readiness states.
2. **Startup Verification Logging**: Diagnostic console output emitted during container initialization.

---

## 2. HTTP Health Endpoint Contract

- **Protocol**: HTTP/1.1
- **Path**: `GET /actuator/health`
- **Port**: Configured `SERVER_PORT` (default `8080`)
- **Authentication**: Unauthenticated for basic health probing

### 2.1 Successful Health Response (HTTP 200 OK)

Returned when the server is healthy and the PostgreSQL connection pool is connected.

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 2.2 Unhealthy / Degraded Response (HTTP 503 Service Unavailable)

Returned if the database becomes unreachable during runtime.

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection"
      }
    }
  }
}
```

---

## 3. Startup Verification Logging Contract

On application startup, `DatabaseConnectionVerifier` (or equivalent bean) verifies the connection and logs structured human-readable diagnostic messages:

### 3.1 Successful Startup Verification (Log Output)

```text
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : ===================================================================
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : Database Connectivity Verified Successfully
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : Product Name    : PostgreSQL
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : Product Version : 16.x
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : Connection Pool : BcmHikariPool (active)
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : Validation Time : 12 ms
INFO  [main] c.b.b.i.c.DatabaseConnectionVerifier : ===================================================================
```

### 3.2 Failure / Unreachable Database (Log Output & Fast Exit)

```text
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : ===================================================================
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : CRITICAL: Database Connectivity Verification FAILED
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : Target URL: jdbc:postgresql://localhost:5432/bcm_db
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : Reason: Connection to localhost:5432 refused
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : Please verify PostgreSQL is running and environment variables are valid.
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : Terminating application bootstrap.
ERROR [main] c.b.b.i.c.DatabaseConnectionVerifier : ===================================================================
```
- Process exits immediately with non-zero exit code (`System.exit(1)` or `ApplicationFailedEvent`).

