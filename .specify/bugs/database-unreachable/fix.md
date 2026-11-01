# Bug Fix: EnvironmentPostProcessor SPI Discovery via spring.factories

- **Slug**: database-unreachable
- **Fixed**: 2026-11-01
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Migrated `DotenvEnvironmentPostProcessor` registration from the unsupported `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` location to `META-INF/spring.factories`. This enables Spring Boot 3 to discover and execute the dotenv environment processor during early bootstrap before datasource and Flyway initialization.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `src/main/resources/META-INF/spring.factories` | added | Registered `DotenvEnvironmentPostProcessor` under `org.springframework.boot.env.EnvironmentPostProcessor` for Spring Boot SPI discovery. |
| `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` | removed | Deleted invalid imports file that was ignored by Spring Boot 3's `EnvironmentPostProcessorApplicationListener`. |
| `src/test/java/com/basketapp/bcmserver/infrastructure/config/DotenvEnvironmentPostProcessorTest.java` | modified | Added tests verifying factory loader SPI registration and full `SpringApplication` bootstrap with `.env` injection. |
| `src/test/java/com/basketapp/bcmserver/infrastructure/config/EnvironmentConfigurationTest.java` | modified | Ensured `DB_QUERY_PARAMS` isolation in discrete datasource binding test to prevent ambient `.env` values from leaking into property tests. |

## Diff Highlights

### `META-INF/spring.factories`
```properties
org.springframework.boot.env.EnvironmentPostProcessor=com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessor
```

### `DotenvEnvironmentPostProcessorTest.java`
```java
@Test
@DisplayName("Should be discoverable via SpringFactoriesLoader under EnvironmentPostProcessor")
void shouldBeDiscoverableViaSpringFactoriesLoader() {
    List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(
            EnvironmentPostProcessor.class,
            getClass().getClassLoader()
    );

    assertTrue(factoryNames.contains(DotenvEnvironmentPostProcessor.class.getName()));
}

@Test
@DisplayName("Should load .env when bootstrapped via SpringApplication SPI")
void shouldLoadEnvWhenBootstrappedViaSpringApplication(@TempDir Path tempDir) throws IOException {
    Path envPath = tempDir.resolve(".env");
    Files.writeString(envPath, "BOOTSTRAP_SPI_KEY=discovered_via_spring_factories\n");
    // bootstraps SpringApplication without manual processor invocation
}
```

## Tests Added or Updated

- `com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessorTest::shouldBeDiscoverableViaSpringFactoriesLoader` — verifies `DotenvEnvironmentPostProcessor` is present in `META-INF/spring.factories` SPI discovery definitions.
- `com.basketapp.bcmserver.infrastructure.config.DotenvEnvironmentPostProcessorTest::shouldLoadEnvWhenBootstrappedViaSpringApplication` — verifies end-to-end Spring Boot application bootstrap loads properties from `.env` via standard Spring factories.
- `com.basketapp.bcmserver.infrastructure.config.EnvironmentConfigurationTest::shouldBindDiscreteDatabaseProperties` — updated to explicitly specify `DB_QUERY_PARAMS=` so ambient `.env` query parameter defaults do not interfere with unit assertions.

## Local Verification

- Commands run:
  - `.\mvnw.cmd test "-Dtest=DotenvEnvironmentPostProcessorTest"` → Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS)
  - `.\mvnw.cmd test "-Dtest=DotenvEnvironmentPostProcessorTest,EnvironmentConfigurationTest"` → Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS)
- Manual checks:
  - Verified `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` removed and directory cleaned.
  - Verified `SpringFactoriesLoader.loadFactoryNames` identifies the registered post-processor.

## Deviations from Assessment

None. The fix strictly implemented the preferred remediation to declare the processor in `META-INF/spring.factories` and deleted the invalid `.imports` file.

## Follow-ups

- Run verification test (`/speckit-bug-test slug=database-unreachable`) to ensure end-to-end runtime reachability with the Supabase credentials configured in `.env`.
