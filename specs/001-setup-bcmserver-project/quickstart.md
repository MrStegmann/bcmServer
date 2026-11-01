# Quickstart Validation Guide: Setup BCMServer Project

**Feature**: `001-setup-bcmserver-project`  
**Date**: 2026-10-29  
**Status**: Completed  

---

## 1. Prerequisites

Before running the validation scenarios, ensure your environment has:
- **Java Development Kit (JDK)**: Version 21 (`java -version`)
- **Docker & Docker Compose**: For running the local PostgreSQL container (`docker compose version`)
- **Maven**: 3.9+ or use the bundled `./mvnw` / `mvnw.cmd` wrapper script

---

## 2. Setup Commands

### Step 1: Clone and Enter Repository
```bash
cd /path/to/bcmServer
```

### Step 2: Prepare Environment File
Copy the provided `.env.example` to `.env`:
```bash
cp .env.example .env
```
*(On Windows PowerShell)*:
```powershell
Copy-Item .env.example .env
```

### Step 3: Start Local PostgreSQL Service
Start the PostgreSQL container via Docker Compose:
```bash
docker compose up -d
```
Verify the container is healthy:
```bash
docker compose ps
```

---

## 3. Validation Scenarios

### Scenario A: Clean Project Build & Dependency Resolution (Priority P3)
Tests Requirement FR-001, FR-002, and Success Criteria SC-001.

**Command**:
```bash
# On Linux/macOS
./mvnw clean compile

# On Windows
.\mvnw.cmd clean compile
```

**Expected Outcome**:
- Maven downloads parent dependencies (Spring Boot 3.4.x, PostgreSQL driver, JPA, Actuator).
- Compilation succeeds targeting Java 21 byte code.
- Build finishes with `BUILD SUCCESS` and exit code `0`.

---

### Scenario B: Successful Server Startup & DB Verification (Priority P1)
Tests Requirement FR-003, FR-004, FR-007, and Success Criteria SC-002.

**Command**:
```bash
# On Linux/macOS
./mvnw spring-boot:run

# On Windows
.\mvnw.cmd spring-boot:run
```

**Expected Outcome**:
1. Spring Boot initializes within 15 seconds.
2. HikariCP pool `BcmHikariPool` establishes connection to PostgreSQL.
3. The following diagnostic message is logged:
   ```text
   INFO ... Database Connectivity Verified Successfully
   INFO ... Product Name    : PostgreSQL
   INFO ... Connection Pool : BcmHikariPool (active)
   ```
4. Querying the health endpoint returns HTTP 200:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Output: `{"status":"UP","components":{"db":{"status":"UP",...}}}`
5. Inspecting PostgreSQL confirms **no tables were modified or auto-generated** (`ddl-auto: none`).

---

### Scenario C: Environment-Isolated Configuration (Priority P2)
Tests Requirement FR-004, FR-005, and Success Criteria SC-003.

**Verification**:
1. Inspect `src/main/resources/application.yml` to confirm no plain-text database credentials exist.
2. Override credentials at runtime via environment variables:
   ```bash
   # Custom database name override test
   DB_PORT=5432 DB_NAME=bcm_db ./mvnw spring-boot:run
   ```
3. Application establishes connection using the overridden values.

---

### Scenario D: Edge Case - Database Unreachable / Fail-Fast
Tests Edge Case requirements and Success Criteria SC-004.

**Command**:
1. Stop the PostgreSQL container:
   ```bash
   docker compose stop
   ```
2. Attempt to start the server:
   ```bash
   ./mvnw spring-boot:run
   ```

**Expected Outcome**:
1. HikariCP times out within 10 seconds (`connection-timeout: 10000`).
2. An informative error message is logged explaining that the database is unreachable at `localhost:5432`.
3. The application terminates execution with a non-zero exit code within 10 seconds.
4. Restart PostgreSQL to restore normal development state:
   ```bash
   docker compose start
   ```

