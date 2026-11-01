# Quickstart & Verification Guide: Users Endpoints & Contracts

**Feature Branch**: `002-users-endpoints-contracts`  
**Date**: 2026-11-01  

---

## 1. Prerequisites

1. **Java 21**: Verify with `java -version`.
2. **PostgreSQL 16**: Running via Docker Compose or locally:
   ```bash
   docker compose up -d postgres
   ```
3. **Environment Setup**: Ensure `.env` is configured (or uses default fallbacks in `application.yml`):
   ```bash
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=bcm_db
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   JWT_SECRET=bcmServerSecretKeyForJwtAuthenticationMustBeAtLeast32BytesLong!
   ```

---

## 2. Starting the Application

Run the server with the local/dev profile:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Verify the server is running on `http://localhost:8080/actuator/health`.

---

## 3. End-to-End Validation Scenarios (cURL)

### Scenario 1: Register a New User (201 Created)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_sarah","password":"StrongPassword2026!"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 201 Created`
- Header: `Location: /api/v1/users/1`
- Body:
  ```json
  {
    "id": 1,
    "username": "coach_sarah",
    "status": "ACTIVE",
    "createdAt": "2026-11-01T10:00:00Z"
  }
  ```
- **CRITICAL**: Verify `password` / `passwordHash` is **NOT** present in the JSON body.

---

### Scenario 2: Registration Syntax / Missing Field Validation (400 Bad Request)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"","password":""}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 400 Bad Request`
- Content-Type: `application/problem+json`
- Body details validation errors on empty username and empty password.

---

### Scenario 3: Duplicate Registration Collision (409 Conflict)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_sarah","password":"AnotherPassword2026!"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 409 Conflict`
- Content-Type: `application/problem+json`
- Detail: "Username 'coach_sarah' is already registered".

---

### Scenario 4: Semantic Password Complexity Failure (422 Unprocessable Entity)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"weak_pass_user","password":"weakpass"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 422 Unprocessable Entity`
- Content-Type: `application/problem+json`
- Body explains semantic failure (e.g., password does not satisfy minimum complexity rules).

---

### Scenario 5: Registration Abuse Throttling (429 Too Many Requests)

**Command**:
```bash
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"bot_user_$i\",\"password\":\"StrongPassword2026!\"}"
done
```

**Expected Outcome**:
- Subsequent requests exceed rate limit and return `HTTP/1.1 429 Too Many Requests`.
- Response contains header `Retry-After: 60`.
- Body: RFC 7807 problem detail for rate limit exceeded.

---

### Scenario 6: Authenticate / Login (200 OK)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_sarah","password":"StrongPassword2026!"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 200 OK`
- Body contains:
  ```json
  {
    "token": "<JWT_TOKEN_STRING>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "coach_sarah",
      "status": "ACTIVE"
    }
  }
  ```
- Store token for subsequent requests:
  ```bash
  TOKEN="<JWT_TOKEN_STRING>"
  ```

---

### Scenario 7: Login Malformed / Missing Fields (400 Bad Request)

**Command**:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":""}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 400 Bad Request`
- Content-Type: `application/problem+json`
- Problem detail indicates missing credentials.

---

### Scenario 8: Login Invalid Credentials & Anti-Enumeration (401 Unauthorized)

**Command**:
```bash
# Test with non-existent user
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"non_existent_user","password":"SomePassword123!"}'

# Test with valid username but wrong password
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_sarah","password":"WrongPassword!"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 401 Unauthorized`
- Content-Type: `application/problem+json`
- Detail: `"Invalid credentials"` (Identical generic message for both cases, preventing username enumeration).

---

### Scenario 9: Login Attempt on Locked/Inactive Account (403 Forbidden)

**Command**:
```bash
# Given an account with status = 'LOCKED'
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"locked_coach","password":"CorrectPassword123!"}'
```

**Expected Outcome**:
- Status: `HTTP/1.1 403 Forbidden`
- Content-Type: `application/problem+json`
- Detail: `"Account is locked or inactive. Contact support."`

---

### Scenario 10: Login Brute-Force Rate Limiting (429 Too Many Requests)

**Command**:
```bash
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"target_user","password":"WrongPassword!"}'
done
```

**Expected Outcome**:
- High-frequency failed attempts return `HTTP/1.1 429 Too Many Requests` with `Retry-After: 300`.

---

### Scenario 11: Protected Profile Access & Ownership Checks (200 OK / 403 Forbidden)

**Command**:
```bash
# Get own profile
curl -i -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"

# Attempt to modify another user's profile
curl -i -X PUT http://localhost:8080/api/v1/users/999 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"hacked_user"}'
```

**Expected Outcome**:
- `GET /me`: Returns `200 OK` with user profile.
- `PUT /999`: Returns `403 Forbidden` with RFC 7807 problem detail.

---

### Scenario 12: Account Deletion (204 No Content)

**Command**:
```bash
curl -i -X DELETE http://localhost:8080/api/v1/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Status: `HTTP/1.1 204 No Content`.
- Subsequent login or retrieval attempts return `401 Unauthorized` or `404 Not Found`.

---

## 4. Automated Test Verification

Run all unit, slice, and integration tests via Maven:

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Key Verification Checks:
1. `BcmServerApplicationTests` loads context with zero errors.
2. Inbound/Outbound unit tests run without Spring context bootstrap.
3. Controller boundary tests (`@WebMvcTest`) verify 400 Bad Request on syntax/missing fields and 422 Unprocessable Entity on semantic rule violations.
4. Rate limiting tests verify 429 Too Many Requests with `Retry-After` headers.
5. Security tests verify generic 401 on credential failure and 403 on locked accounts.
6. Exception handler tests verify 500 Internal Server Error captures unexpected faults safely.
7. Repository integration tests verify BCrypt hashed storage and Flyway schema integrity.
