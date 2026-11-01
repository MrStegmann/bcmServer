# Bug Verification: Unexpected error during transaction (relation "teams" does not exist)

- **Slug**: teams-relation-missing
- **Tested**: 2026-10-31T12:37:00+01:00
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug no longer reproduces: the application server connects cleanly to PostgreSQL with Flyway enabled by default, applying pending migrations (`V2__create_teams_table.sql` and `V3__create_players_table.sql`). Authenticated `GET /api/v1/teams` and `POST /api/v1/teams` calls succeed with HTTP 200/201 and persist team entities as expected. The full test suite of 233 tests passed with 0 failures.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Database Schema Check | `docker exec bcm-postgres psql -U postgres -d bcm_db -c "\dt"` | pass | `teams` and `players` relations are present in `bcm_db`. |
| Flyway History Verification | `docker exec bcm-postgres psql -U postgres -d bcm_db -c "select installed_rank, version, description, success from flyway_schema_history;"` | pass | Versions 1, 2, and 3 are marked `success = t`. |
| Reproduction (post-fix) | Authenticated `GET /api/v1/teams` & `POST /api/v1/teams` with valid JWT | pass | Returned HTTP 200 / 201; successfully created team "Cadete A" (ID 1) and retrieved it without SQL errors. |
| New / updated tests | `.\mvnw.cmd test "-Dtest=EnvironmentConfigurationTest,BcmServerApplicationTests"` | pass | 4 tests run, 0 failures, 0 errors. Verified `spring.flyway.enabled` defaults to `"true"`. |
| Regression suite | `.\mvnw.cmd test` | pass | 233 tests run, 0 failures, 0 errors, 0 skipped. |
| Actuator Health Check | `Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -Method Get` | pass | Server status `UP` with database component healthy. |

## Output Excerpts

### Post-fix API Reproduction Check
```text
Registered new user: verify_teams_user_2
Obtained JWT token: eyJhbGciOiJIUzM...
GET /api/v1/teams initial count: 0
POST /api/v1/teams success! Created Team ID: 1, Name: Cadete A, Category: Cadete
GET /api/v1/teams after creation count: 1
Team in list: ID=1, Name=Cadete A
```

### Direct Database Relations
```text
                 List of relations
 Schema |         Name          | Type  |  Owner   
--------+-----------------------+-------+----------
 public | flyway_schema_history | table | postgres
 public | players               | table | postgres
 public | teams                 | table | postgres
 public | users                 | table | postgres
(4 rows)
```

### Full Regression Suite
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 233, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  29.283 s
[INFO] Finished at: 2026-10-31T12:34:24+01:00
[INFO] ------------------------------------------------------------------------
```

## Residual Risks

- Existing live container instances or external deployments running prior binaries will require restarting the Spring Boot process once to trigger the default Flyway migration cycle against their database instance.

## Recommendation

Close the bug — verified end-to-end. Both automated test suites and live HTTP/database reproduction confirmed that Flyway executes migrations by default and the `/teams` endpoints process queries without relation errors.

