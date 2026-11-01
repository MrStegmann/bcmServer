# Quickstart Validation Guide: Player Contracts and Endpoints

**Feature**: `004-players-endpoints-contracts`  
**Status**: Draft  
**Date**: 2026-10-31  

This guide provides runnable scenarios to validate the `players` CRUD endpoints end-to-end, testing authentication precedence, pre-database validation, relational integrity, and standard REST responses.

---

## Prerequisites

1. **PostgreSQL & Redis running**:
   ```bash
   docker compose up -d
   ```
2. **bcmServer Application running**:
   ```bash
   ./mvnw spring-boot:run
   ```
   (Server listens on default port `8080`)
3. **CLI Tools**: `curl` and `jq` installed for running commands and inspecting JSON.

---

## Step 0: Setup Auth Token & Parent Team

Before interacting with protected player endpoints, register/login to obtain a valid JWT token, and create a team to serve as the foreign key target (`team_id`).

```bash
# Register test user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_steve","password":"Password123!"}'

# Login to retrieve JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_steve","password":"Password123!"}' | jq -r '.token')

echo "Bearer Token: $TOKEN"

# Create a parent team (team_id will usually be 1)
TEAM_RES=$(curl -s -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Chicago Bulls","category":"Senior Pro"}')

TEAM_ID=$(echo "$TEAM_RES" | jq -r '.id')
echo "Created Team ID: $TEAM_ID"
```

---

## Scenario 1: Verify Token Validation Precedence (401 Before Validation)

Send a malformed payload (missing all required fields) **WITHOUT** a JWT token:

```bash
curl -i -X POST http://localhost:8080/api/v1/players \
  -H "Content-Type: application/json" \
  -d '{"number":""}'
```

**Expected Outcome**:
- **HTTP Status**: `401 Unauthorized`
- **Body**: RFC 7807 `ProblemDetail` with status `401`.
- **Precedence Check**: Must NOT return `400 Bad Request`. Security filter rejects the request prior to payload inspection.

---

## Scenario 2: Create a Player (Happy Path - 201 Created)

Send an authenticated request with valid player data referencing the created team:

```bash
CREATE_RES=$(curl -s -i -X POST http://localhost:8080/api/v1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"teamId\":$TEAM_ID,\"firstName\":\"Michael\",\"lastName\":\"Jordan\",\"number\":\"23\",\"jerseyName\":\"JORDAN\"}")

echo "$CREATE_RES"
```

**Expected Outcome**:
- **HTTP Status**: `201 Created`
- **Header**: `Location: /api/v1/players/<id>`
- **Response Body**:
  ```json
  {
    "id": 1,
    "teamId": 1,
    "firstName": "Michael",
    "lastName": "Jordan",
    "number": "23",
    "jerseyName": "JORDAN",
    "createdAt": "..."
  }
  ```

---

## Scenario 3: Pre-Database Validation on Missing/Blank Fields (400 Bad Request)

Send an authenticated request with invalid attributes (blank first name, missing number, negative team ID):

```bash
curl -s -i -X POST http://localhost:8080/api/v1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"teamId\":-5,\"firstName\":\"   \",\"lastName\":\"Jordan\",\"number\":\"\"}"
```

**Expected Outcome**:
- **HTTP Status**: `400 Bad Request`
- **Body**: RFC 7807 `ProblemDetail` with title `"Validation Failed"`, listing violation reasons in `invalidParams` for `teamId`, `firstName`, and `number`.
- **Database Verification**: Zero database queries or inserts performed.

---

## Scenario 4: Foreign Key Validation for Non-Existent Team (404 Not Found)

Submit a valid player creation payload with a `teamId` that does not exist in the database:

```bash
curl -s -i -X POST http://localhost:8080/api/v1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"teamId":99999,"firstName":"Scottie","lastName":"Pippen","number":"33","jerseyName":"PIPPEN"}'
```

**Expected Outcome**:
- **HTTP Status**: `404 Not Found`
- **Body**: RFC 7807 `ProblemDetail` stating that the referenced team was not found, handled cleanly without SQL foreign key constraint crashes.

---

## Scenario 5: Retrieve Player by ID and Non-Existent ID (200 OK & 404 Not Found)

Retrieve the player created in Scenario 2:

```bash
# Retrieve existing player (id: 1)
curl -s -i -X GET http://localhost:8080/api/v1/players/1 \
  -H "Authorization: Bearer $TOKEN"

# Attempt to retrieve non-existent player (id: 99999)
curl -s -i -X GET http://localhost:8080/api/v1/players/99999 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Existing ID: `200 OK` with player JSON matching the contract in [contracts/players-api.yaml](contracts/players-api.yaml).
- Non-existent ID: `404 Not Found` with RFC 7807 `ProblemDetail`.

---

## Scenario 6: List All Players & Filter by Team ID (200 OK)

Test roster queries:

```bash
# List all players
curl -s -X GET http://localhost:8080/api/v1/players \
  -H "Authorization: Bearer $TOKEN" | jq .

# Filter players by team ID
curl -s -X GET "http://localhost:8080/api/v1/players?teamId=$TEAM_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected Outcome**:
- `200 OK` with JSON array of player records belonging to the requested filter.

---

## Scenario 7: Update Player Profile (200 OK)

Update the jersey display name or jersey number:

```bash
curl -s -i -X PUT http://localhost:8080/api/v1/players/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"teamId\":$TEAM_ID,\"firstName\":\"Michael\",\"lastName\":\"Jordan\",\"number\":\"45\",\"jerseyName\":\"M. JORDAN\"}"
```

**Expected Outcome**:
- **HTTP Status**: `200 OK`
- **Body**: Updated player record showing `"number": "45"` and `"jerseyName": "M. JORDAN"`.

---

## Scenario 8: Delete Player & Verify Deletion (204 No Content & 404)

Permanently delete the player:

```bash
# Delete player
curl -s -i -X DELETE http://localhost:8080/api/v1/players/1 \
  -H "Authorization: Bearer $TOKEN"

# Verify subsequent retrieval returns 404
curl -s -i -X GET http://localhost:8080/api/v1/players/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Deletion response: `204 No Content` with empty body.
- Subsequent `GET /players/1`: `404 Not Found`.

