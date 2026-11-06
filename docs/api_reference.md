# bcmServer - API Reference Manual

Complete REST API specification for **bcmServer** (Basket Club Manager Server).

---

## 1. General Principles

### Base URL
- **Local Development**: `http://localhost:8080`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.0 Spec**: `http://localhost:8080/v3/api-docs`

### Headers & Authentication
Unless otherwise stated, all API requests should provide:
```http
Content-Type: application/json
Accept: application/json
```

For protected routes, include the Bearer JWT token obtained from `/api/v1/auth/login`:
```http
Authorization: Bearer <your-jwt-token>
```

### Rate Limiting
Public endpoints (`/api/v1/auth/register` and `/api/v1/auth/login`) are protected by an in-memory sliding-window rate limiter:
- Default quota: **5 requests per 60-second window** per client IP.
- When exceeded, the server returns HTTP `429 Too Many Requests` with the `Retry-After: 60` response header.

### Error Format (RFC 7807 Problem Details)
All error responses return `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for request payload",
  "instance": "/api/v1/teams",
  "invalidParams": {
    "name": "Team name cannot be null, empty, or blank"
  }
}
```

---

## 2. Authentication (`/api/v1/auth`)

### 2.1 Register User Account
Registers a new user and returns their public profile.

- **Method**: `POST`
- **Endpoint**: `/api/v1/auth/register`
- **Auth**: None (Public)
- **Rate Limit**: Yes (5 req / 60s)

#### Request Body
```json
{
  "username": "coach_john",
  "password": "SecurePassword123!"
}
```

| Field | Type | Validation Rules |
| :--- | :--- | :--- |
| `username` | string | Mandatory, 3–50 chars, alphanumeric + underscores, unique |
| `password` | string | Mandatory, 8–100 chars, must contain at least 1 letter and 1 digit |

#### Responses
- **`201 Created`**: User successfully registered.
  - Header: `Location: /api/v1/users/1`
  ```json
  {
    "id": 1,
    "username": "coach_john",
    "status": "ACTIVE",
    "createdAt": "2026-11-06T15:30:00Z"
  }
  ```
- **`400 Bad Request`**: Request payload failed syntactic validation.
- **`409 Conflict`**: Username is already registered.
- **`422 Unprocessable Entity`**: Semantic validation failed (e.g. password rules).
- **`429 Too Many Requests`**: Rate limit exceeded.

#### Curl Example
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_john","password":"SecurePassword123!"}'
```

---

### 2.2 User Login
Authenticates user credentials and issues a signed stateless JWT bearer token.

- **Method**: `POST`
- **Endpoint**: `/api/v1/auth/login`
- **Auth**: None (Public)
- **Rate Limit**: Yes (5 req / 60s)

#### Request Body
```json
{
  "username": "coach_john",
  "password": "SecurePassword123!"
}
```

#### Responses
- **`200 OK`**: Authentication successful.
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjb2FjaF9qb2huIiwiaWQiOjEsImlhdCI6MTc2MjQ0MzAwMCwiZXhwIjoxNzYyNTI5NDAwfQ.signature",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "coach_john",
      "status": "ACTIVE",
      "createdAt": "2026-11-06T15:30:00Z"
    }
  }
  ```
- **`401 Unauthorized`**: Invalid username or password.
- **`403 Forbidden`**: Account is `LOCKED` or `INACTIVE`.
- **`429 Too Many Requests`**: Rate limit exceeded.

#### Curl Example
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_john","password":"SecurePassword123!"}'
```

---

## 3. Users (`/api/v1/users`)

### 3.1 Get Current User Profile
Retrieves the profile of the caller based on their JWT claims.

- **Method**: `GET`
- **Endpoint**: `/api/v1/users/me`
- **Auth**: `Bearer <token>`

#### Responses
- **`200 OK`**:
  ```json
  {
    "id": 1,
    "username": "coach_john",
    "status": "ACTIVE",
    "createdAt": "2026-11-06T15:30:00Z"
  }
  ```
- **`401 Unauthorized`**: Missing or invalid token.

#### Curl Example
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.2 Get User by ID
- **Method**: `GET`
- **Endpoint**: `/api/v1/users/{id}`
- **Auth**: `Bearer <token>`

#### Responses
- **`200 OK`**: User profile retrieved.
- **`404 Not Found`**: Target user ID does not exist.

---

### 3.3 Update User Profile
- **Method**: `PUT`
- **Endpoint**: `/api/v1/users/{id}`
- **Auth**: `Bearer <token>` (Enforces self-ownership)

#### Request Body
```json
{
  "username": "coach_john_updated",
  "password": "NewSecurePassword456!"
}
```

#### Responses
- **`200 OK`**: Profile updated successfully.
- **`403 Forbidden`**: Cannot update other users' accounts.
- **`409 Conflict`**: Target username already taken.

---

### 3.4 Delete User Account
- **Method**: `DELETE`
- **Endpoint**: `/api/v1/users/{id}`
- **Auth**: `Bearer <token>` (Enforces self-ownership)

#### Responses
- **`204 No Content`**: User deleted.
- **`403 Forbidden`**: Cannot delete other users' accounts.

---

## 4. Teams (`/api/v1/teams` & `/teams`)

### 4.1 List All Teams
- **Method**: `GET`
- **Endpoint**: `/api/v1/teams` (alias: `/teams`)
- **Auth**: `Bearer <token>`

#### Responses
- **`200 OK`**:
  ```json
  [
    {
      "id": 1,
      "name": "Senior Masculino A",
      "category": "Senior 1a Division",
      "createdAt": "2026-11-06T15:30:00Z"
    }
  ]
  ```

---

### 4.2 Create Team
- **Method**: `POST`
- **Endpoint**: `/api/v1/teams` (alias: `/teams`)
- **Auth**: `Bearer <token>`

#### Request Body
```json
{
  "name": "Senior Masculino A",
  "category": "Senior 1a Division"
}
```

| Field | Type | Rules |
| :--- | :--- | :--- |
| `name` | string | Mandatory, 1–100 chars |
| `category` | string | Optional, max 50 chars |

#### Responses
- **`201 Created`**: Team created. Header `Location: /api/v1/teams/1`.
- **`400 Bad Request`**: Name is empty or missing.

#### Curl Example
```bash
curl -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Junior Femenino","category":"U17 Preferente"}'
```

---

### 4.3 Get Team by ID
- **Method**: `GET`
- **Endpoint**: `/api/v1/teams/{id}`
- **Auth**: `Bearer <token>`

---

### 4.4 Update Team
- **Method**: `PUT`
- **Endpoint**: `/api/v1/teams/{id}`
- **Auth**: `Bearer <token>`

#### Request Body
```json
{
  "name": "Senior Masculino Autonomico",
  "category": "Liga Autonomica"
}
```

---

### 4.5 Delete Team
- **Method**: `DELETE`
- **Endpoint**: `/api/v1/teams/{id}`
- **Auth**: `Bearer <token>`

> [!WARNING]
> Deleting a team cascades to all players, games, and player stats belonging to that team.

#### Responses
- **`204 No Content`**: Successfully deleted.
- **`404 Not Found`**: Team ID not found.

---

## 5. Players (`/api/v1/players` & `/players`)

### 5.1 List Players
- **Method**: `GET`
- **Endpoint**: `/api/v1/players` (alias: `/players`)
- **Query Parameters**:
  - `teamId` (optional, long): Filter players belonging to a specific team.
- **Auth**: `Bearer <token>`

#### Responses
- **`200 OK`**:
  ```json
  [
    {
      "id": 1,
      "teamId": 1,
      "firstName": "Michael",
      "lastName": "Jordan",
      "number": "23",
      "jerseyName": "JORDAN",
      "createdAt": "2026-11-06T15:30:00Z"
    }
  ]
  ```

---

### 5.2 Create Player
- **Method**: `POST`
- **Endpoint**: `/api/v1/players` (alias: `/players`)
- **Auth**: `Bearer <token>`

#### Request Body
```json
{
  "teamId": 1,
  "firstName": "Michael",
  "lastName": "Jordan",
  "number": "23",
  "jerseyName": "JORDAN"
}
```

| Field | Type | Rules |
| :--- | :--- | :--- |
| `teamId` | long | Mandatory, positive integer, team must exist |
| `firstName` | string | Mandatory, 1–100 chars |
| `lastName` | string | Mandatory, 1–100 chars |
| `number` | string | Mandatory, 1–10 chars |
| `jerseyName`| string | Optional, max 50 chars |

#### Responses
- **`201 Created`**: Header `Location: /api/v1/players/1`.
- **`400 Bad Request`**: Validation failed.
- **`404 Not Found`**: Referenced `teamId` does not exist.

---

### 5.3 Get Player by ID
- **Method**: `GET`
- **Endpoint**: `/api/v1/players/{id}`
- **Auth**: `Bearer <token>`

---

### 5.4 Update Player
- **Method**: `PUT`
- **Endpoint**: `/api/v1/players/{id}`
- **Auth**: `Bearer <token>`

---

### 5.5 Delete Player
- **Method**: `DELETE`
- **Endpoint**: `/api/v1/players/{id}`
- **Auth**: `Bearer <token>`

---

## 6. Games (`/api/v1/games`, `/api/games`, `/games`)

### 6.1 List Games
- **Method**: `GET`
- **Endpoint**: `/api/v1/games` (aliases: `/api/games`, `/games`)
- **Query Parameters**:
  - `teamId` (optional, long): Filter games by team.
  - `status` (optional, string): Filter by status (`pending`, `live`, `finished`).
- **Auth**: `Bearer <token>`

---

### 6.2 Schedule / Create Game
- **Method**: `POST`
- **Endpoint**: `/api/v1/games`
- **Auth**: `Bearer <token>`

#### Request Body
```json
{
  "teamId": 1,
  "opponent": "CB Estudiantes",
  "round": "1",
  "date": "2026-11-15T18:00:00Z",
  "status": "pending",
  "resultC1": 0,
  "resultC2": 0,
  "resultC3": 0,
  "resultC4": 0,
  "resultExtra": 0,
  "resultC1Opponent": 0,
  "resultC2Opponent": 0,
  "resultC3Opponent": 0,
  "resultC4Opponent": 0,
  "resultExtraOpponent": 0,
  "foulsC1": 0,
  "foulsC2": 0,
  "foulsC3": 0,
  "foulsC4": 0,
  "foulsExtra": 0,
  "foulsC1Opponent": 0,
  "foulsC2Opponent": 0,
  "foulsC3Opponent": 0,
  "foulsC4Opponent": 0,
  "foulsExtraOpponent": 0
}
```

---

### 6.3 Get Game by ID (Scoreboard)
- **Method**: `GET`
- **Endpoint**: `/api/v1/games/{id}`
- **Auth**: `Bearer <token>`

#### Responses
- **`200 OK`**:
  ```json
  {
    "id": 1,
    "teamId": 1,
    "opponent": "CB Estudiantes",
    "round": "1",
    "date": "2026-11-15T18:00:00Z",
    "status": "live",
    "createdAt": "2026-11-06T15:30:00Z",
    "resultC1": 22,
    "resultC2": 19,
    "resultC3": 25,
    "resultC4": 0,
    "resultExtra": 0,
    "resultC1Opponent": 18,
    "resultC2Opponent": 20,
    "resultC3Opponent": 17,
    "resultC4Opponent": 0,
    "resultExtraOpponent": 0,
    "foulsC1": 3,
    "foulsC2": 4,
    "foulsC3": 2,
    "foulsC4": 0,
    "foulsExtra": 0,
    "foulsC1Opponent": 4,
    "foulsC2Opponent": 5,
    "foulsC3Opponent": 3,
    "foulsC4Opponent": 0,
    "foulsExtraOpponent": 0
  }
  ```

---

### 6.4 Update Game (Scores, Fouls, Status)
- **Method**: `PUT`
- **Endpoint**: `/api/v1/games/{id}`
- **Auth**: `Bearer <token>`

---

### 6.5 Delete Game
- **Method**: `DELETE`
- **Endpoint**: `/api/v1/games/{id}`
- **Auth**: `Bearer <token>`

---

## 7. Player Stats (`/api/v1/player-stats`, `/api/player-stats`, `/player-stats`)

### 7.1 List Player Stats
- **Method**: `GET`
- **Endpoint**: `/api/v1/player-stats`
- **Query Parameters**:
  - `gameId` (optional, long): Filter stats by game ID.
  - `playerId` (optional, long): Filter stats by player ID.
- **Auth**: `Bearer <token>`

---

### 7.2 Record Box-Score Stats
- **Method**: `POST`
- **Endpoint**: `/api/v1/player-stats`
- **Auth**: `Bearer <token>`

#### Request Body
```json
{
  "gameId": 1,
  "playerId": 1,
  "minutes": 32,
  "seconds": 45,
  "t1a": 6,
  "t1i": 5,
  "t2a": 12,
  "t2i": 8,
  "t3a": 5,
  "t3i": 3,
  "oreb": 2,
  "dreb": 6,
  "asis": 7,
  "rec": 3,
  "per": 2,
  "falt": 2,
  "valuation": 34
}
```

| Field | Meaning | Constraints |
| :--- | :--- | :--- |
| `gameId` / `playerId` | References | Positive integers, referenced records must exist |
| `minutes` | Play time mins | `>= 0` |
| `seconds` | Play time secs | `0` to `59` |
| `t1a` / `t1i` | Free throws (attempted / made) | `>= 0`, `t1i <= t1a` |
| `t2a` / `t2i` | 2PT Field goals (attempted / made) | `>= 0`, `t2i <= t2a` |
| `t3a` / `t3i` | 3PT Field goals (attempted / made) | `>= 0`, `t3i <= t3a` |
| `oreb` / `dreb` | Offensive & Defensive Rebounds | `>= 0` |
| `asis` | Assists | `>= 0` |
| `rec` | Steals / Recoveries | `>= 0` |
| `per` | Turnovers / Ball losses | `>= 0` |
| `falt` | Personal fouls committed | `>= 0` |
| `valuation` | PIR rating | Integer (can be negative) |

#### Responses
- **`201 Created`**: Header `Location: /api/v1/player-stats/1`.
- **`400 Bad Request`**: If made shots exceed attempted shots, seconds > 59, or values negative.
- **`404 Not Found`**: Referenced `gameId` or `playerId` not found.

---

### 7.3 Get Player Stats by ID
- **Method**: `GET`
- **Endpoint**: `/api/v1/player-stats/{id}`
- **Auth**: `Bearer <token>`

---

### 7.4 Update Player Stats
- **Method**: `PUT`
- **Endpoint**: `/api/v1/player-stats/{id}`
- **Auth**: `Bearer <token>`

---

### 7.5 Delete Player Stats
- **Method**: `DELETE`
- **Endpoint**: `/api/v1/player-stats/{id}`
- **Auth**: `Bearer <token>`

---

## 8. Diagnostics & Actuator

| Method | Endpoint | Auth | Purpose |
| :--- | :--- | :---: | :--- |
| `GET` | `/actuator/health` | None | Application and PostgreSQL liveness/readiness probe |
| `GET` | `/actuator/info` | None | Build and git metadata |

