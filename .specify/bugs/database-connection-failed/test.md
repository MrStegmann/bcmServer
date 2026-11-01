# Bug Verification: Supabase Direct Database Connection via IPv6 Tunnel & Baseline Alignment

- **Slug**: database-connection-failed
- **Tested**: 2026-11-01
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The `08001` / `UnknownHostException` failure connecting to the Supabase direct database host (`db.ccthgmjmkdxhpqovbbvk.supabase.co:5432`) is fully resolved. Host network dual-stack IPv6 routing via Cloudflare WARP and JVM IPv6 configuration allow direct socket and JDBC connections to succeed, Flyway baseline alignment (`baseline-version: 5`) succeeds against existing tables, and all 409 regression tests pass.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `Test-NetConnection -ComputerName db.ccthgmjmkdxhpqovbbvk.supabase.co -Port 5432` | pass | Resolved to `2a05:d012:7a:e02:977e:ba49:7c3:2bef` and established TCP connection (`TcpTestSucceeded: True`). |
| New / updated tests | `.\mvnw.cmd test -Dtest=SupabaseDirectConnectivityTest` | pass | Verified direct JDBC connection, PostgreSQL 17.11 metadata, and table discovery (`users`, `teams`, `flyway_schema_history`). |
| Fail-fast test | `.\mvnw.cmd test -Dtest=DatabaseFailFastIntegrationTest` | pass | Rejection test against unreachable port fails fast deterministically in 3.6s without hanging. |
| Regression suite | `.\mvnw.cmd test` | pass | Full suite passed: 409 tests run, 0 failures, 0 errors, 0 skipped. |
| Build / compile check | `.\mvnw.cmd test-compile` | pass | Clean compilation with zero build errors. |

## Output Excerpts

### Network Reachability Check
```text
ComputerName     : db.ccthgmjmkdxhpqovbbvk.supabase.co
RemoteAddress    : 2a05:d012:7a:e02:977e:ba49:7c3:2bef
RemotePort       : 5432
InterfaceAlias   : CloudflareWARP
TcpTestSucceeded : True
```

### Direct JDBC Connectivity Test (`SupabaseDirectConnectivityTest`)
```text
[INFO] Running com.basketapp.bcmserver.infrastructure.config.SupabaseDirectConnectivityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.801 s -- in com.basketapp.bcmserver.infrastructure.config.SupabaseDirectConnectivityTest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Full Regression Suite (`mvn test`)
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 409, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:00 min
```

## Residual Risks

- **Cloudflare WARP Dependency**: The local ISP network does not provide native IPv6 routing. Direct connections to `db.<project-ref>.supabase.co` require Cloudflare WARP to remain connected on the host machine during development (unless Supabase's paid IPv4 add-on is enabled).
- **Supabase Free Tier Inactivity**: Supabase pauses idle free-tier database instances after 7 days; if paused, direct connection attempts will fail until unpaused from the Supabase web dashboard.

## Recommendation

Close the bug — verified end-to-end. Supabase direct connectivity over IPv6 is functional, Flyway migrations align with existing remote database tables, and the full test suite passes with zero regressions.

