# Quickstart Validation Guide: Games Contracts and Endpoints

**Feature**: `005-games-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-31  

This guide provides runnable scenarios to validate the `games` CRUD endpoints end-to-end, testing authentication precedence, pre-database validation, relational integrity, default field values, score updates, and standard REST responses.

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

Before interacting with protected game endpoints, register/login to obtain a valid JWT token, and create a team to serve as the foreign key target (`team_id`).

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

# Create a parent team
TEAM_RES=$(curl -s -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Chicago Bulls","category":"Senior Pro"}')

TEAM_ID=$(echo "$TEAM_RES" | jq -r '.id')
echo "Created Team ID: $TEAM_ID"
```

---

## Scenario 1: Verify Token Validation Precedence (401 Before Validation)

Send a malformed payload (missing all required fields) **WITHOUT** a JWT token to `/api/games`:

```bash
curl -i -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -d '{"opponent":""}'
```

**Expected Outcome**:
- **HTTP Status**: `401 Unauthorized`
- **Body**: RFC 7807 `ProblemDetail` with status `401`.
- **Precedence Check**: Must NOT return `400 Bad Request`. Security filter rejects the request prior to payload inspection.

---

## Scenario 2: Create a Game (Happy Path - 201 Created with Defaults)

Send an authenticated request with valid game data referencing the created team, omitting optional quarter scores, fouls, round, and status:

```bash
CREATE_RES=$(curl -s -i -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"team_id\":$TEAM_ID,\"opponent\":\"Miami Heat\",\"date\":\"2026-11-10T19:00:00Z\"}")

echo "$CREATE_RES"
```

**Expected Outcome**:
- **HTTP Status**: `201 Created`
- **Header**: `Location: /api/games/<id>`
- **Response Body**:
  ```json
  {
    "id": 1,
    "team_id": 1,
    "opponent": "Miami Heat",
    "round": "1",
    "date": "2026-11-10T19:00:00Z",
    "status": "pending",
    "result_c1": 0,
    "result_c2": 0,
    "result_c3": 0,
    "result_c4": 0,
    "result_extra": 0,
    "result_c1_opponent": 0,
    "result_c2_opponent": 0,
    "result_c3_opponent": 0,
    "result_c4_opponent": 0,
    "result_extra_opponent": 0,
    "fouls_c1": 0,
    "fouls_c2": 0,
    "fouls_c3": 0,
    "fouls_c4": 0,
    "fouls_extra": 0,
    "fouls_c1_opponent": 0,
    "fouls_c2_opponent": 0,
    "fouls_c3_opponent": 0,
    "fouls_c4_opponent": 0,
    "fouls_extra_opponent": 0,
    "created_at": "..."
  }
  ```

---

## Scenario 3: Pre-Database Validation on Missing/Blank Fields & Negative Values (400 Bad Request)

Send an authenticated request with invalid attributes (blank opponent, missing date, negative team ID, negative quarter score):

```bash
curl -s -i -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"team_id\":-1,\"opponent\":\"   \",\"date\":\"\",\"result_c1\":-10}"
```

**Expected Outcome**:
- **HTTP Status**: `400 Bad Request`
- **Body**: RFC 7807 `ProblemDetail` with title `"Validation Failed"`, listing violation reasons in `invalidParams` for `team_id`, `opponent`, `date`, and `result_c1`.
- **Database Verification**: Zero database queries or inserts performed.

---

## Scenario 4: Foreign Key Validation for Non-Existent Team (404 Not Found)

Submit a valid game creation payload with a `team_id` that does not exist in the database:

```bash
curl -s -i -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"team_id":99999,"opponent":"Boston Celtics","date":"2026-11-12T20:00:00Z"}'
```

**Expected Outcome**:
- **HTTP Status**: `404 Not Found`
- **Body**: RFC 7807 `ProblemDetail` stating that the referenced team was not found, handled cleanly without database SQL exception crashes.

---

## Scenario 5: Retrieve Game by ID and Non-Existent ID (200 OK & 404 Not Found)

Retrieve the game created in Scenario 2:

```bash
# Retrieve existing game (id: 1)
curl -s -i -X GET http://localhost:8080/api/games/1 \
  -H "Authorization: Bearer $TOKEN"

# Attempt to retrieve non-existent game (id: 99999)
curl -s -i -X GET http://localhost:8080/api/games/99999 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Existing ID: `200 OK` with game JSON matching the contract in [contracts/games-api.yaml](contracts/games-api.yaml).
- Non-existent ID: `404 Not Found` with RFC 7807 `ProblemDetail`.

---

## Scenario 6: List All Games & Filter by Team ID (200 OK)

Test game queries:

```bash
# List all games
curl -s -X GET http://localhost:8080/api/games \
  -H "Authorization: Bearer $TOKEN" | jq .

# Filter games by team ID
curl -s -X GET "http://localhost:8080/api/games?teamId=$TEAM_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected Outcome**:
- `200 OK` with JSON array of game records belonging to the requested filter.

---

## Scenario 7: Update Game Status, Quarter Scores, and Fouls (200 OK)

Update the game match status to `live`, setting quarter 1 and quarter 2 points and fouls:

```bash
curl -s -i -X PUT http://localhost:8080/api/games/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"team_id\":$TEAM_ID,\"opponent\":\"Miami Heat\",\"round\":\"1\",\"date\":\"2026-11-10T19:00:00Z\",\"status\":\"live\",\"result_c1\":24,\"result_c2\":22,\"result_c3\":0,\"result_c4\":0,\"result_extra\":0,\"result_c1_opponent\":20,\"result_c2_opponent\":25,\"result_c3_opponent\":0,\"result_c4_opponent\":0,\"result_extra_opponent\":0,\"fouls_c1\":4,\"fouls_c2\":3,\"fouls_c3\":0,\"fouls_c4\":0,\"fouls_extra\":0,\"fouls_c1_opponent\":5,\"fouls_c2_opponent\":4,\"fouls_c3_opponent\":0,\"fouls_c4_opponent\":0,\"fouls_extra_opponent\":0}"
```

**Expected Outcome**:
- **HTTP Status**: `200 OK`
- **Body**: Updated game record with `"status": "live"`, `"result_c1": 24`, `"result_c1_opponent": 20`, and updated fouls.

---

## Scenario 8: Delete Game & Verify Deletion (204 No Content & 404)

Permanently delete the game:

```bash
# Delete game
curl -s -i -X DELETE http://localhost:8080/api/games/1 \
  -H "Authorization: Bearer $TOKEN"

# Verify subsequent retrieval returns 404
curl -s -i -X GET http://localhost:8080/api/games/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- Deletion response: `204 No Content` with empty body.
- Subsequent `GET /api/games/1`: `404 Not Found`.

