# Interface Contract: Users Endpoints (`/api/v1/users`)

**Feature Branch**: `002-users-endpoints-contracts`  
**Base Path**: `/api/v1/users`  
**Access**: Protected (Requires `Authorization: Bearer <JWT>`)  
**Standard**: RFC 7807 Problem Details for all error responses  

---

## 1. `GET /api/v1/users/me`

Retrieves the profile of the currently authenticated user based on the JWT token claims.

### Request

- **Headers**:
  - `Authorization: Bearer <jwt-token>`

### Responses

#### `200 OK`
- **Body**:
  ```json
  {
    "id": 1,
    "username": "coach_john",
    "createdAt": "2026-10-29T14:30:00Z"
  }
  ```

#### `401 Unauthorized`
Returned when JWT is missing, invalid, or expired.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/unauthorized",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Full authentication is required to access this resource",
    "instance": "/api/v1/users/me"
  }
  ```

---

## 2. `GET /api/v1/users/{id}`

Retrieves public user profile by numeric user ID.

### Request

- **Path Parameters**:
  - `id` (`integer / int64`): ID of the user to retrieve
- **Headers**:
  - `Authorization: Bearer <jwt-token>`

### Responses

#### `200 OK`
- **Body**:
  ```json
  {
    "id": 1,
    "username": "coach_john",
    "createdAt": "2026-10-29T14:30:00Z"
  }
  ```

#### `401 Unauthorized`
Returned when token is missing or invalid.

#### `404 Not Found`
Returned when user ID does not exist in the database.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/user-not-found",
    "title": "Not Found",
    "status": 404,
    "detail": "User with ID 999 not found",
    "instance": "/api/v1/users/999"
  }
  ```

---

## 3. `PUT /api/v1/users/{id}`

Updates an existing user's username and/or password. Enforces self-service authorization (users can only update their own account).

### Request

- **Path Parameters**:
  - `id` (`integer / int64`): ID of the user
- **Headers**:
  - `Authorization: Bearer <jwt-token>`
  - `Content-Type: application/json`
- **Body** (fields are individually optional, at least one required):
  ```json
  {
    "username": "coach_john_updated",
    "password": "NewSecurePassword456!"
  }
  ```

### Responses

#### `200 OK`
- **Body**:
  ```json
  {
    "id": 1,
    "username": "coach_john_updated",
    "createdAt": "2026-10-29T14:30:00Z"
  }
  ```

#### `400 Bad Request`
Returned when supplied fields violate validation constraints (e.g. password < 8 chars).

#### `401 Unauthorized`
Returned when token is missing, expired, or invalid.

#### `403 Forbidden`
Returned when an authenticated user attempts to modify an account ID other than their own.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/forbidden",
    "title": "Forbidden",
    "status": 403,
    "detail": "You do not have permission to update account ID 2",
    "instance": "/api/v1/users/2"
  }
  ```

#### `404 Not Found`
Returned when user ID does not exist.

#### `409 Conflict`
Returned when the updated username is already registered to another user.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/username-conflict",
    "title": "Conflict",
    "status": 409,
    "detail": "Username 'coach_john_updated' is already taken",
    "instance": "/api/v1/users/1"
  }
  ```

---

## 4. `DELETE /api/v1/users/{id}`

Permanently deletes the user account. Enforces self-service authorization (users can only delete their own account).

### Request

- **Path Parameters**:
  - `id` (`integer / int64`): ID of the user to delete
- **Headers**:
  - `Authorization: Bearer <jwt-token>`

### Responses

#### `204 No Content`
Account deleted successfully. Empty response body.

#### `401 Unauthorized`
Returned when token is missing or invalid.

#### `403 Forbidden`
Returned when an authenticated user attempts to delete an account ID other than their own.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/forbidden",
    "title": "Forbidden",
    "status": 403,
    "detail": "You do not have permission to delete account ID 2",
    "instance": "/api/v1/users/2"
  }
  ```

#### `404 Not Found`
Returned when user ID does not exist.

