# Interface Contract: Authentication Endpoints (`/api/v1/auth`)

**Feature Branch**: `002-users-endpoints-contracts`  
**Base Path**: `/api/v1/auth`  
**Access**: Public (No authentication required)  
**Standard**: RFC 7807 Problem Details for all error responses  

---

## 1. `POST /api/v1/auth/register`

Creates a new user account with hashed password and returns public user profile.

### Request

- **Headers**:
  - `Content-Type: application/json`
- **Body**:
  ```json
  {
    "username": "coach_john",
    "password": "SecurePassword123!"
  }
  ```
- **Constraints**:
  - `username`: Non-blank, 3 to 50 characters, alphanumeric with optional `_`, `-`, `.`.
  - `password`: Non-blank, min 8 characters, max 100 characters.

### Responses

#### `201 Created`
Returned when user is successfully registered.
- **Headers**:
  - `Content-Type: application/json`
  - `Location: /api/v1/users/{id}`
- **Body**:
  ```json
  {
    "id": 1,
    "username": "coach_john",
    "createdAt": "2026-10-29T14:30:00Z"
  }
  ```

#### `400 Bad Request`
Returned when payload fails basic validation (e.g., missing required fields, invalid syntax).
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/validation-error",
    "title": "Bad Request",
    "status": 400,
    "detail": "Input validation failed for 1 field(s)",
    "instance": "/api/v1/auth/register",
    "invalidParams": [
      {
        "field": "username",
        "message": "username must not be blank"
      }
    ]
  }
  ```

#### `409 Conflict`
Returned when a user attempts to register with an identifier (username or email) that is already registered.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/username-conflict",
    "title": "Conflict",
    "status": 409,
    "detail": "Username 'coach_john' is already registered",
    "instance": "/api/v1/auth/register"
  }
  ```

#### `422 Unprocessable Entity`
Returned when request syntax is well-formed, but semantic validation / business logic rules fail (e.g., password does not meet security complexity criteria).
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/semantic-validation-error",
    "title": "Unprocessable Entity",
    "status": 422,
    "detail": "Password does not meet minimum security requirements",
    "instance": "/api/v1/auth/register",
    "invalidParams": [
      {
        "field": "password",
        "message": "Password is too weak. Must contain uppercase, lowercase, numbers, and special symbols"
      }
    ]
  }
  ```

#### `429 Too Many Requests`
Returned when registration rate limits are exceeded for a given client IP address.
- **Headers**:
  - `Content-Type: application/problem+json`
  - `Retry-After: 60`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/too-many-requests",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Registration rate limit exceeded. Please try again later.",
    "instance": "/api/v1/auth/register"
  }
  ```

#### `500 Internal Server Error`
Returned on unexpected server failures (e.g., database connection loss, internal service crash).
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/internal-server-error",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "An unexpected error occurred while processing registration",
    "instance": "/api/v1/auth/register"
  }
  ```

---

## 2. `POST /api/v1/auth/login`

Authenticates a user against stored password hash and returns a signed stateless JWT token.

### Request

- **Headers**:
  - `Content-Type: application/json`
- **Body**:
  ```json
  {
    "username": "coach_john",
    "password": "SecurePassword123!"
  }
  ```

### Responses

#### `200 OK`
Returned when credentials are verified.
- **Headers**:
  - `Content-Type: application/json`
- **Body**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "coach_john",
      "createdAt": "2026-10-29T14:30:00Z"
    }
  }
  ```

#### `400 Bad Request`
Returned when payload is malformed or missing required credentials (`username`/`email`, `password`).
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/validation-error",
    "title": "Bad Request",
    "status": 400,
    "detail": "Required fields are missing or payload is malformed",
    "instance": "/api/v1/auth/login"
  }
  ```

#### `401 Unauthorized`
Returned when credentials are invalid (wrong password or account not found). Returns a generic message to prevent account enumeration.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/invalid-credentials",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Invalid credentials",
    "instance": "/api/v1/auth/login"
  }
  ```

#### `403 Forbidden`
Returned when credentials are correct, but the account is locked, inactive, or pending verification.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/account-forbidden",
    "title": "Forbidden",
    "status": 403,
    "detail": "Account is locked or inactive. Contact support.",
    "instance": "/api/v1/auth/login"
  }
  ```

#### `429 Too Many Requests`
Returned when rate limits are triggered due to repeated failed login attempts from an IP address or for an account.
- **Headers**:
  - `Content-Type: application/problem+json`
  - `Retry-After: 300`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/too-many-requests",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Too many failed login attempts. Please try again after 5 minutes.",
    "instance": "/api/v1/auth/login"
  }
  ```

#### `500 Internal Server Error`
Returned when unexpected server errors or JWT generation exceptions occur.
- **Headers**: `Content-Type: application/problem+json`
- **Body**:
  ```json
  {
    "type": "https://api.basketapp.com/errors/internal-server-error",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "An unexpected error occurred during authentication",
    "instance": "/api/v1/auth/login"
  }
  ```
