package com.basketapp.bcmserver.infrastructure.config;

import com.basketapp.bcmserver.BcmServerApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseFailFastIntegrationTest {

    @Test
    @DisplayName("Application should fail fast within 10 seconds when PostgreSQL is unreachable")
    void shouldFailFastWhenDatabaseIsUnreachable() {
        Instant start = Instant.now();

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

        Duration elapsed = Duration.between(start, Instant.now());
        assertTrue(elapsed.toSeconds() < 10,
                "Fail-fast shutdown must complete in under 10 seconds, but took: " + elapsed.toMillis() + " ms");
    }
}
