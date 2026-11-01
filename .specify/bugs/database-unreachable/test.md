# Bug Verification: Database Configuration Ignored / Dotenv SPI Discovery Failure

- **Slug**: database-unreachable
- **Tested**: 2026-11-01
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug is verified as fixed. Spring Boot now discovers `DotenvEnvironmentPostProcessor` via `META-INF/spring.factories` during early application bootstrap. The application no longer ignores `.env` configuration or falls back to `localhost:5432`; it correctly binds the database host and credentials defined in the `.env` file. All 408 tests in the regression suite passed with zero regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `.\mvnw.cmd spring-boot:run` | pass | Application discovered `DotenvEnvironmentPostProcessor` via SPI and targeted `db.ccthgmjmkdxhpqovbbvk.supabase.co` from `.env` instead of `localhost:5432`. |
| New / updated tests | `.\mvnw.cmd test "-Dtest=DotenvEnvironmentPostProcessorTest,EnvironmentConfigurationTest"` | pass | All 12 tests passed, confirming `SpringFactoriesLoader` SPI discovery and full `SpringApplication` bootstrap. |
| Regression suite | `.\mvnw.cmd test` | pass | Full test suite passed (408 tests run, 0 failures). |
| Lint / type-check | `.\mvnw.cmd test-compile` | pass | Compilation completed with zero warnings/errors. |

## Output Excerpts

### Post-fix Reproduction (`.\mvnw.cmd spring-boot:run`)
The connection attempt correctly targets the hostname loaded from `.env` instead of defaulting to `localhost:5432`:
```text
2026-11-01T09:58:49.812+01:00  INFO 11360 --- [bcmServer] [           main] com.zaxxer.hikari.HikariDataSource       : BcmHikariPool - Starting...
...
Caused by: java.net.UnknownHostException: db.ccthgmjmkdxhpqovbbvk.supabase.co
	at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:567) ~[na:na]
	at org.postgresql.core.PGStream.createSocket(PGStream.java:260) ~[postgresql-42.7.5.jar:42.7.5]
	at org.postgresql.core.v3.ConnectionFactoryImpl.tryConnect(ConnectionFactoryImpl.java:140) ~[postgresql-42.7.5.jar:42.7.5]
```

### New & Updated Tests (`DotenvEnvironmentPostProcessorTest,EnvironmentConfigurationTest`)
```text
[INFO] Running com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessorTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.080 s -- in com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessorTest
[INFO] Running com.basketapp.bcmserver.infrastructure.config.EnvironmentConfigurationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 18.81 s -- in com.basketapp.bcmserver.infrastructure.config.EnvironmentConfigurationTest
[INFO] Results:
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Regression Suite (`.\mvnw.cmd test`)
```text
[INFO] Results:
[INFO] Tests run: 408, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  32.192 s
```

## Residual Risks

- **IPv6 Network Requirement for Supabase Direct Host**: As noted in `.env`, `db.<project-ref>.supabase.co` only resolves to IPv6 (`AAAA`). On environments lacking an active IPv6 internet route, the host will not resolve (`UnknownHostException`). Users on IPv4-only networks must uncomment the IPv4 pooler configuration (`aws-0-eu-central-1.pooler.supabase.com` on port 6543) in `.env`.

## Recommendation

Close the bug — verified end-to-end. The `DotenvEnvironmentPostProcessor` is properly registered via `META-INF/spring.factories` and is automatically discovered by Spring Boot without programmatic wiring.
