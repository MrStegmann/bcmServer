# Quickstart Validation Guide: Player Stats Contracts and Endpoints

**Feature**: `006-player-stats-endpoints-contracts`  
**Status**: Completed  
**Date**: 2026-10-31  

This guide provides runnable end-to-end scenarios to validate the `player_stats` CRUD endpoints, confirming authentication precedence, pre-database validation, relational integrity, default field values, filtering, updates, and cascading deletion.

---

## Prerequisites

1. **Docker Services Running (PostgreSQL & Redis)**:
   ```bash
   docker compose up -d
   ```
2. **bcmServer Running**:
   ```bash
   ./mvnw spring-boot:run
   ```
   (Server operates by default on port `8080`)
3. **CLI Tools**: `curl` and `jq` installed for executing requests and parsing JSON responses.

---

## Step 0: Setup Auth Token, Team, Game, and Player

Before interacting with protected player stats endpoints, register/login to obtain a valid JWT token, and create the required parent team, game, and player foreign key targets.

```bash
# 1. Register test user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"stat_admin","password":"Password123!"}'

# 2. Login to retrieve JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"stat_admin","password":"Password123!"}' | jq -r '.token')

echo "Bearer Token: $TOKEN"

# 3. Create a parent team
TEAM_RES=$(curl -s -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Los Angeles Lakers","category":"Senior"}')
TEAM_ID=$(echo "$TEAM_RES" | jq -r '.id')
echo "Team ID: $TEAM_ID"

# 4. Create a parent game
GAME_RES=$(curl -s -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"team_id\":$TEAM_ID,\"opponent\":\"Golden State Warriors\",\"date\":\"2026-11-15T20:00:00Z\"}")
GAME_ID=$(echo "$GAME_RES" | jq -r '.id')
echo "Game ID: $GAME_ID"

# 5. Create a parent player
PLAYER_RES=$(curl -s -X POST http://localhost:8080/api/v1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"team_id\":$TEAM_ID,\"first_name\":\"LeBron\",\"last_name\":\"James\",\"number\":\"23\",\"jersey_name\":\"KING\"}")
PLAYER_ID=$(echo "$PLAYER_RES" | jq -r '.id')
echo "Player ID: $PLAYER_ID"
```

---

## Scenario 1: Verify Token Validation Precedence (401 Before Validation)

Send a malformed payload (missing mandatory `game_id` and `player_id`) **WITHOUT** a JWT token to `/api/player-stats`:

```bash
curl -i -X POST http://localhost:8080/api/player-stats \
  -H "Content-Type: application/json" \
  -d '{"minutes": -5}'
```

**Expected Outcome**:
- **HTTP Status**: `401 Unauthorized`
- **Body**: RFC 7807 `ProblemDetail` with status `401`.
- **Precedence Check**: Must NOT return `400 Bad Request`. Security filter rejects the request prior to payload inspection.

---

## Scenario 2: Verify Pre-Database Validation (400 Bad Request)

Send an authenticated request with invalid data (missing required foreign keys and negative counters):

```bash
curl -i -X POST http://localhost:8080/api/player-stats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"game_id": null, "player_id": -1, "minutes": -10, "seconds": 75}'
```

**Expected Outcome**:
- **HTTP Status**: `400 Bad Request`
- **Body**: RFC 7807 `ProblemDetail` highlighting `invalidParams` for `game_id`, `player_id`, `minutes`, and `seconds`. Zero database records created.

---

## Scenario 3: Verify Foreign Key Referential Check (404 Not Found)

Send an authenticated request referencing a non-existent `game_id`:

```bash
curl -i -X POST http://localhost:8080/api/player-stats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"game_id\": 999999, \"player_id\": $PLAYER_ID, \"minutes\": 15}"
```

**Expected Outcome**:
- **HTTP Status**: `404 Not Found`
- **Body**: RFC 7807 `ProblemDetail` indicating the referenced game does not exist.

---

## Scenario 4: Create Player Stats (Happy Path - 201 Created with Defaults)

Create player statistics with complete shooting metrics, rebounds, assists, and valuation:

```bash
CREATE_RES=$(curl -s -i -X POST http://localhost:8080/api/player-stats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"game_id\": $GAME_ID,
    \"player_id\": $PLAYER_ID,
    \"minutes\": 34,
    \"seconds\": 20,
    \"t1a\": 6,
    \"t1i\": 5,
    \"t2a\": 12,
    \"t2i\": 8,
    \"t3a\": 6,
    \"t3i\": 3,
    \"oreb\": 2,
    \"dreb\": 7,
    \"asis\": 9,
    \"rec\": 3,
    \"per\": 2,
    \"falt\": 2,
    \"valuation\": 32
  }")

echo "$CREATE_RES"
```

**Expected Outcome**:
- **HTTP Status**: `201 Created`
- **Header**: `Location: /api/player-stats/<id>`
- **Response Body**:
  ```json
  {
    "id": 1,
    "game_id": 1,
    "player_id": 1,
    "minutes": 34,
    "seconds": 20,
    "t1a": 6,
    "t1i": 5,
    "t2a": 12,
    "t2i": 8,
    "t3a": 6,
    "t3i": 3,
    "oreb": 2,
    "dreb": 7,
    "asis": 9,
    "rec": 3,
    "per": 2,
    "falt": 2,
    "valuation": 32
  }
  ```

---

## Scenario 5: Retrieve Stats by ID and Collection with Filtering (200 OK)

### 5.1 Fetch by Unique ID
```bash
curl -s -X GET http://localhost:8080/api/player-stats/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 5.2 List Filtered by Game ID
```bash
curl -s -X GET "http://localhost:8080/api/player-stats?gameId=$GAME_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 5.3 List Filtered by Player ID
```bash
curl -s -X GET "http://localhost:8080/api/player-stats?playerId=$PLAYER_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected Outcome**:
- **HTTP Status**: `200 OK` with JSON array or object containing matching player statistics.

---

## Scenario 6: Update Player Stats (200 OK)

Update statistics to correct playing time and adjust fouls:

```bash
curl -s -X PUT http://localhost:8080/api/player-stats/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"game_id\": $GAME_ID,
    \"player_id\": $PLAYER_ID,
    \"minutes\": 36,
    \"seconds\": 0,
    \"t1a\": 6,
    \"t1i\": 5,
    \"t2a\": 12,
    \"t2i\": 8,
    \"t3a\": 6,
    \"t3i\": 3,
    \"oreb\": 2,
    \"dreb\": 8,
    \"asis\": 10,
    \"rec\": 3,
    \"per\": 2,
    \"falt\": 3,
    \"valuation\": 35
  }" | jq .
```

**Expected Outcome**:
- **HTTP Status**: `200 OK` reflecting updated `minutes: 36`, `dreb: 8`, `asis: 10`, `falt: 3`, and `valuation: 35`.

---

## Scenario 7: Delete Player Stats (204 No Content)

```bash
curl -i -X DELETE http://localhost:8080/api/player-stats/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Outcome**:
- **HTTP Status**: `204 No Content`
- Verification: Subsequent `GET http://localhost:8080/api/player-stats/1` returns `404 Not Found`.

---

## Reference Links
- [API Contract Specification](contracts/player-stats-api.yaml)
- [Data Model & Entities](data-model.md)
- [Research & Architectural Decisions](research.md)

