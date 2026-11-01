# Bug Assessment: Database Unreachable / Dotenv EnvironmentPostProcessor SPI Discovery Failure

- **Slug**: database-unreachable
- **Created**: 2026-11-01
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

```text
The backend does not connect to the database and initialize a localhost database instead connecting to supabase defined in `.env`.
Follow-up from verification (test.md):
While DotenvEnvironmentPostProcessor and dotenv-java were introduced in the previous fix, the application still fell back to localhost:5432 during .\mvnw.cmd spring-boot:run because Spring Boot did not discover DotenvEnvironmentPostProcessor from META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports.
```

## Symptom

When launching the application (e.g. via `.\mvnw.cmd spring-boot:run` or running `BcmServerApplication`), `bcmServer` ignores the database configuration in the root `.env` file and falls back to `localhost:5432/bcm_db`. If no local PostgreSQL is active on port 5432, startup terminates with `Connection to localhost:5432 refused` during Flyway initialization:

```text
Unable to obtain connection from database: Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

## Reproduction

1. Place Supabase credentials in the root `.env`:
   ```dotenv
   DB_HOST=aws-0-eu-central-1.pooler.supabase.com
   DB_PORT=6543
   DB_NAME=postgres
   DB_USERNAME=postgres.ccthgmjmkdxhpqovbbvk
   DB_PASSWORD=nU4goTgNHwsvTrvV
   DB_QUERY_PARAMS=?sslmode=require
   ```
2. Ensure no matching environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`) are exported in the host terminal environment.
3. Start the application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. Observe startup logs:
   - HikariCP logs connection attempt to `jdbc:postgresql://localhost:5432/bcm_db`.
   - Startup fails with connection refused on `localhost:5432`.
   - `DotenvEnvironmentPostProcessor` is never invoked by Spring Boot.

## Suspected Code Paths

- `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports:1` — Spring Boot 3 `EnvironmentPostProcessorApplicationListener` loads processors via `SpringFactoriesLoader.forResourceLocation("META-INF/spring.factories")`. It does not load `EnvironmentPostProcessor` from `META-INF/spring/*.imports` (which is reserved for `@AutoConfiguration`).
- `src/main/resources/META-INF/spring.factories` — Missing file. The processor `com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessor` must be declared here under `org.springframework.boot.env.EnvironmentPostProcessor`.
- `src/test/java/com/basketapp/bcmserver/infrastructure/config/DotenvEnvironmentPostProcessorTest.java:1-85` — Tests instantiate `DotenvEnvironmentPostProcessor` directly (`new DotenvEnvironmentPostProcessor().postProcessEnvironment(...)`), masking the fact that Spring Boot's factory loader was not discovering the class.

## Root Cause Hypothesis

In Spring Boot 3.x, `EnvironmentPostProcessor` implementations are loaded early in the application lifecycle by `EnvironmentPostProcessorApplicationListener` using `SpringFactoriesLoader` targeting `META-INF/spring.factories`. 

Although `AutoConfiguration` classes moved to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` in Spring 2.7 / 3.x, `EnvironmentPostProcessor` registration continues to be loaded via `META-INF/spring.factories`. Because `DotenvEnvironmentPostProcessor` was registered only in `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`, Spring Boot never instantiates or executes it during standard bootstrap (`SpringApplication.run`), causing the application to fall back to the default `localhost:5432` values defined in `application.yml`. Confidence: high.

## Proposed Remediation

**Preferred**:
1. Create `src/main/resources/META-INF/spring.factories` and register:
   ```properties
   org.springframework.boot.env.EnvironmentPostProcessor=com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessor
   ```
2. Remove the invalid `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` file.
3. Add a Spring Boot bootstrap integration test verifying that `SpringFactoriesLoader` finds `DotenvEnvironmentPostProcessor` and that a bootstrapped `SpringApplication` loads `.env` properties into `Environment` automatically without manual processor instantiation.

**Alternatives**:
- Register `DotenvEnvironmentPostProcessor` programmatically in `BcmServerApplication.main(String[] args)` via `new SpringApplicationBuilder(BcmServerApplication.class).environmentPostProcessors(...)`. (Trade-off: Bypasses standard Spring SPI and does not run when tests use `@SpringBootTest` unless duplicated).

**Files likely to change**:
- `src/main/resources/META-INF/spring.factories` (new)
- `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` (delete)
- `src/test/java/com/basketapp/bcmserver/infrastructure/config/DotenvEnvironmentPostProcessorTest.java` (add SPI loading verification)

**Tests to add or update**:
- Test asserting `SpringFactoriesLoader.loadFactories(EnvironmentPostProcessor.class, ...)` returns an instance of `DotenvEnvironmentPostProcessor`.
- Verification that `SpringApplicationBuilder` bootstraps the environment with `.env` values when loaded via standard Spring factories.

## Risks & Considerations

- **Packaging in JAR**: Ensure `META-INF/spring.factories` is included in the output JAR resources without collision with other starters.
- **Multiple EnvironmentPostProcessors**: Ensure order defined by `Ordered.HIGHEST_PRECEDENCE + 9` executes ahead of `ConfigDataEnvironmentPostProcessor` while preserving existing Spring system property overrides.

## Open Questions

- None. The mechanism of `EnvironmentPostProcessor` discovery in Spring Boot 3 via `META-INF/spring.factories` is confirmed and reproducible.
