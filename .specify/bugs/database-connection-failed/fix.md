# Bug Fix: Supabase Direct Database Connection via IPv6 Tunnel & Baseline Alignment

- **Slug**: database-connection-failed
- **Fixed**: 2026-11-01
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Resolved the `08001` / `UnknownHostException` failure connecting to the Supabase direct database host (`db.ccthgmjmkdxhpqovbbvk.supabase.co:5432`) by activating Cloudflare WARP dual-stack IPv6 routing on the host OS, configuring JVM options (`.mvn/jvm.config`), aligning Flyway baseline versioning (`baseline-version: 5`) to match pre-existing Supabase schema tables, and isolating localhost loopback in `DatabaseFailFastIntegrationTest`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `.mvn/jvm.config` | added | Configured `-Djava.net.preferIPv4Stack=false` and `-Djava.net.preferIPv6Addresses=true` for Maven and JVM bootstrap. |
| `src/main/resources/application.yml` | modified | Added `spring.flyway.baseline-version: ${SPRING_FLYWAY_BASELINE_VERSION:5}` to align Flyway with existing tables on the live database. |
| `src/test/java/com/basketapp/bcmserver/infrastructure/config/SupabaseDirectConnectivityTest.java` | added | Added live direct connectivity and schema integration test for Supabase PostgreSQL direct connection. |
| `src/test/java/com/basketapp/bcmserver/infrastructure/config/DatabaseFailFastIntegrationTest.java` | modified | Bound `DB_HOST=127.0.0.1` explicitly to guarantee fail-fast socket rejection without WAN TCP packet timeout. |

## Diff Highlights

### `.mvn/jvm.config`
```properties
-Djava.net.preferIPv4Stack=false
-Djava.net.preferIPv6Addresses=true
```

### `src/main/resources/application.yml`
```yaml
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:true}
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: ${SPRING_FLYWAY_BASELINE_VERSION:5}
    validate-on-migrate: true
```

### `DatabaseFailFastIntegrationTest.java`
```java
assertThrows(Throwable.class, () -> {
    new SpringApplicationBuilder(BcmServerApplication.class)
            .web(WebApplicationType.NONE)
            .properties(
                    "DB_HOST=127.0.0.1",
                    "DB_PORT=65432",
                    "spring.datasource.hikari.connection-timeout=1500"
            )
            .run();
});
```

## Tests Added or Updated

- `com.basketapp.bcmserver.infrastructure.config.SupabaseDirectConnectivityTest::shouldConnectDirectlyToSupabaseWhenHostIsResolvable` — verifies TCP/JDBC direct connection to Supabase (`db.ccthgmjmkdxhpqovbbvk.supabase.co:5432`), database metadata (PostgreSQL 17.11), and schema tables (`users`, `teams`, `players`, `games`, `player_stats`, `flyway_schema_history`).
- `com.basketapp.bcmserver.infrastructure.config.DatabaseFailFastIntegrationTest::shouldFailFastWhenDatabaseIsUnreachable` — updated to target loopback `127.0.0.1:65432` ensuring deterministic fail-fast behavior (< 3s) independent of ambient `.env` WAN hosts.

## Local Verification

- Commands run:
  - `warp-cli connect` → Status: Connected, Network: healthy (activated global IPv6 transit on Windows).
  - `Test-NetConnection -ComputerName db.ccthgmjmkdxhpqovbbvk.supabase.co -Port 5432` → `TcpTestSucceeded: True` via `CloudflareWARP` interface.
  - `.\mvnw.cmd test -Dtest=SupabaseDirectConnectivityTest` → `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS in 0.82s).
  - `.\mvnw.cmd test -Dtest=DatabaseFailFastIntegrationTest` → `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS in 2.38s).
  - `.\mvnw.cmd spring-boot:run` → Application started successfully, verified database connection in 67 ms (`Database Connectivity Verified Successfully: PostgreSQL 17.11`), and `/actuator/health` responded with `{"status":"UP"}`.
  - `.\mvnw.cmd test` → Full regression suite: `Tests run: 409, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS).
- Manual checks:
  - Checked `http://localhost:8080/actuator/health` via `Invoke-RestMethod`: returned `status: UP` with healthy `db` component.

## Deviations from Assessment

None. The assessment identified the missing IPv6 transit as the root cause of `UnknownHostException` for Supabase's direct endpoint. Activating Cloudflare WARP on the host and setting JVM IPv6 preference flags enabled seamless name resolution and socket connectivity to `db.ccthgmjmkdxhpqovbbvk.supabase.co:5432`. Flyway baseline version was also configured to version 5 to prevent collision with pre-existing tables in the active Supabase project.

## Follow-ups

- Run `/speckit-bug-test slug=database-connection-failed` to generate the formal test verification report.
- Ensure Cloudflare WARP remains connected during development sessions requiring direct connections to Supabase.
