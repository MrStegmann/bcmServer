# Bug Verification: Login Endpoint Connection Error (Socket Hang Up)

- **Slug**: login-wrong-port
- **Tested**: 2026-10-30
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Verified that the backend application correctly exposes authentication endpoints on port 8080 and that the reported `socket hang up` was caused by targeting PostgreSQL on port 5432 instead of the web server. All authentication unit tests and the full application test suite passed with zero failures and zero regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Inspected `application.yml` and endpoint routes | pass | Verified `server.port` defaults to 8080 while database uses port 5432; socket hang up occurs exclusively when non-protocol HTTP traffic targets PostgreSQL port 5432. |
| New / updated tests | `.\mvnw.cmd test -Dtest=AuthControllerTest` | pass | 6/6 tests passed in 5.569s validating `AuthController` and `/api/v1/auth/login`. |
| Regression suite | `.\mvnw.cmd test` | pass | Full suite passed: 68 tests run, 0 failures, 0 errors, 0 skipped. |
| Lint / type-check | `.\mvnw.cmd test-compile` | pass | Verified compilation succeeded with no errors. |

## Output Excerpts

### AuthControllerTest
```text
[INFO] Running com.basketapp.bcmserver.infrastructure.adapter.in.web.AuthControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.569 s -- in com.basketapp.bcmserver.infrastructure.adapter.in.web.AuthControllerTest
[INFO] BUILD SUCCESS
```

### Full Regression Suite
```text
[INFO] Results:
[INFO] Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  30.156 s
```

## Residual Risks

- Postman or external HTTP client environments must ensure `api_url` or `server_port` points to `8080` rather than the database port `5432`.

## Recommendation

Close the bug — verified. The issue is confirmed as invalid for the backend service; no application code defect exists, and all controller endpoints and integration suites pass cleanly.

