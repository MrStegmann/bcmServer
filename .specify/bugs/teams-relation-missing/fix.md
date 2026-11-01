# Bug Fix: Enable Flyway schema migrations by default

- **Slug**: teams-relation-missing
- **Fixed**: 2026-10-31T12:30:00+01:00
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Defaulted `spring.flyway.enabled` to `true` (`${SPRING_FLYWAY_ENABLED:true}`) in `application.yml` so database migrations automatically execute on startup across default environments, ensuring the `teams` relation (and subsequent tables) exist before Hibernate/JPA queries them.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `src/main/resources/application.yml` | modified | Changed `spring.flyway.enabled` default value from `false` to `true` via `${SPRING_FLYWAY_ENABLED:true}`. |
| `src/test/java/com/basketapp/bcmserver/BcmServerApplicationTests.java` | modified | Explicitly set `spring.flyway.enabled=false` on `@SpringBootTest` properties to isolate context loads with a mocked `DataSource`. |
| `src/test/java/com/basketapp/bcmserver/infrastructure/config/EnvironmentConfigurationTest.java` | modified | Configured `SPRING_FLYWAY_ENABLED=false` on discrete property-binding tests, and added a test asserting `spring.flyway.enabled` defaults to `"true"`. |

## Diff Highlights

```yaml
# src/main/resources/application.yml
   flyway:
-    enabled: ${SPRING_FLYWAY_ENABLED:false}
+    enabled: ${SPRING_FLYWAY_ENABLED:true}
     locations: classpath:db/migration
```

```java
// src/test/java/com/basketapp/bcmserver/BcmServerApplicationTests.java
-@SpringBootTest(properties = "bcm.database.verification.enabled=false")
+@SpringBootTest(properties = {
+        "bcm.database.verification.enabled=false",
+        "spring.flyway.enabled=false"
+})
 class BcmServerApplicationTests {
```

```java
// src/test/java/com/basketapp/bcmserver/infrastructure/config/EnvironmentConfigurationTest.java
+    @Test
+    @DisplayName("Should default spring.flyway.enabled to true")
+    void shouldDefaultFlywayEnabledToTrue() {
+        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
+                .web(WebApplicationType.NONE)
+                .run()) {
+            assertEquals("true", context.getEnvironment().getProperty("spring.flyway.enabled"));
+        }
+    }
```

## Tests Added or Updated

- `src/test/java/com/basketapp/bcmserver/infrastructure/config/EnvironmentConfigurationTest.java::shouldDefaultFlywayEnabledToTrue` — Pins down that `spring.flyway.enabled` defaults to `true` in standard environment configuration.
- `src/test/java/com/basketapp/bcmserver/BcmServerApplicationTests.java::contextLoads` — Updated test properties to maintain test slice isolation when mocking `DataSource`.

## Local Verification

- Commands run:
  - `.\mvnw.cmd test` → Build success: 233 tests run, 0 failures, 0 errors.
  - `.\mvnw.cmd spring-boot:run` → Flyway validated and migrated schema from version 1 to version 3 (`V2__create_teams_table.sql` and `V3__create_players_table.sql` applied).
  - `docker exec bcm-postgres psql -U postgres -d bcm_db -c "\dt"` → Confirmed `teams` and `players` relations now exist in `bcm_db`.
  - `docker exec bcm-postgres psql -U postgres -d bcm_db -c "select installed_rank, version, description, success from flyway_schema_history;"` → Confirmed ranks 1, 2, and 3 are recorded as successful migrations.

## Deviations from Assessment

None. The remediation followed the preferred plan to set `spring.flyway.enabled: ${SPRING_FLYWAY_ENABLED:true}` in `src/main/resources/application.yml` and add verification tests.

## Follow-ups

- Restart any long-running local server instance currently on port 8080 so it runs against the migrated database.

