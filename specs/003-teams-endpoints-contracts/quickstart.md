# Quickstart Validation Guide: Team Contracts and Endpoints

**Feature**: `003-teams-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-30  

This guide provides runnable scenarios to validate the team CRUD endpoints end-to-end, testing both authentication precedence and business functionality.

---

## Prerequisites

1. **PostgreSQL & Redis services running**:
   ```bash
   docker compose up -d
   ```
2. **bcmServer Application running**:
   ```bash
   ./mvnw spring-boot:run
   ```
   (Server listens on default port `8080`)

3. **Dependencies**: `curl` and `jq` installed for CLI testing.

---

## Step 0: Obtain JWT Authentication Token

Register a test user (or log in) to receive a bearer token:

```bash
# Register test user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_test","password":"Password123!"}'

# Login to retrieve JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_test","password":"Password123!"}' | jq -r '.token')

echo "Bearer Token: $TOKEN"
```

---

## Scenario 1: Verify Token Validation Precedence (401 Before Validation)

Send a malformed payload (missing name) **WITHOUT** a valid JWT token:

```bash
curl -i -X POST http://localhost:8080/api/v1/teams \
  -H "Content-Type: application/json" \
  -d '{"category":"U16"}'
```

**Expected Outcome**:
- HTTP Status: `401 Unauthorized`
- Body: Standard RFC 7807 `ProblemDetail` with `status: 401`.
- Crucially: NOT `400 Bad Request`. Security filter rejects the request before request body parsing or validation.

---

## Scenario 2: Create a Team (Happy Path - 201 Created)

```bash
CREATE_RES=$(curl -s -i -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Cadete Femenino A","category":"U16 Federado"}')

echo "$CREATE_RES"
```

**Expected Outcome**:
- HTTP Status: `201 Created`
- Header `Location`: Contains path to created team (e.g. `/api/v1/teams/1`)
- Body JSON:
  ```json
  {
    "id": 1,
    "name": "Cadete Femenino A",
    "category": "U16 Federado",
    "createdAt": "..."
  }
  ```

---

## Scenario 3: Pre-Database Validation on Missing Name (400 Bad Request)

Send an invalid payload with an authenticated user:

```bash
curl -s -i -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"  ","category":"U16"}'
```

**Expected Outcome**:
- HTTP Status: `400 Bad Request`
- Body JSON:
  ```json
  {
    "type": "about:blank",
    "title": "Validation Failed",
    "status": 400,
    "detail": "Validation failed for request payload",
    "invalidParams": {
      "name": "Team name is required and cannot be blank"
    }
  }
  ```
- Database check: No record created or database exception raised.

---

## Scenario 4: Retrieve Team by ID and Collection (200 OK)

```bash
# Get single team
curl -s -i -X GET http://localhost:8080/api/v1/teams/1 \
  -H "Authorization: Bearer $TOKEN"

# List all teams
curl -s -i -X GET http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- HTTP Status: `200 OK`
- List response contains an array of active teams matching [contracts/teams-api.yaml](file:///contracts/teams-api.yaml).

---

## Scenario 5: Update Team (200 OK)

```bash
curl -s -i -X PUT http://localhost:8080/api/v1/teams/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Cadete Femenino Oro","category":"U16 Oro"}'
```

**Expected Outcome**:
- HTTP Status: `200 OK`
- Body reflects the updated team attributes.

---

## Scenario 6: Non-Existent Team & Malformed ID (404 and 400)

```bash
# Non-existent ID -> 404 Not Found
curl -s -i -X GET http://localhost:8080/api/v1/teams/9999 \
  -H "Authorization: Bearer $TOKEN"

# Malformed ID -> 400 Bad Request (not 500)
curl -s -i -X GET http://localhost:8080/api/v1/teams/invalid-id \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- 9999 returns `404 Not Found` with `ProblemDetail`.
- `invalid-id` returns `400 Bad Request` with `ProblemDetail` (no database execution).

---

## Scenario 7: Delete Team (204 No Content)

```bash
# Delete team
curl -s -i -X DELETE http://localhost:8080/api/v1/teams/1 \
  -H "Authorization: Bearer $TOKEN"

# Verify subsequent retrieval returns 404
curl -s -i -X GET http://localhost:8080/api/v1/teams/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Deletion returns `204 No Content`.
- Subsequent retrieval returns `404 Not Found`.

---

## Automated Verification Suite

Run automated unit, slice, and integration tests:

```bash
# Run unit and slice tests
./mvnw test -Dtest=Team*Test,UserControllerTest,SecurityConfigTest
```

