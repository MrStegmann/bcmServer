package com.basketapp.bcmserver.infrastructure.config;

import com.basketapp.bcmserver.BcmServerApplication;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnvironmentConfigurationTest {

    @Test
    @DisplayName("Should bind discrete database environment properties into DataSource")
    void shouldBindDiscreteDatabaseProperties() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BcmServerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "DB_HOST=custom-db-host",
                        "DB_PORT=5433",
                        "DB_NAME=custom_db",
                        "DB_QUERY_PARAMS=",
                        "DB_USERNAME=custom_user",
                        "DB_PASSWORD=custom_pass",
                        "bcm.database.verification.enabled=false",
                        "SPRING_FLYWAY_ENABLED=false"
                )
                .run()) {

            DataSource dataSource = context.getBean(DataSource.class);
            assertNotNull(dataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            assertEquals("jdbc:postgresql://custom-db-host:5433/custom_db", hikariDataSource.getJdbcUrl());
            assertEquals("custom_user", hikariDataSource.getUsername());
            assertEquals("custom_pass", hikariDataSource.getPassword());
        }
    }

    @Test
    @DisplayName("Should append DB_QUERY_PARAMS to discrete database URL")
    void shouldAppendDbQueryParams() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BcmServerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "DB_HOST=custom-db-host",
                        "DB_PORT=5433",
                        "DB_NAME=custom_db",
                        "DB_QUERY_PARAMS=?sslmode=require",
                        "bcm.database.verification.enabled=false",
                        "SPRING_FLYWAY_ENABLED=false"
                )
                .run()) {

            DataSource dataSource = context.getBean(DataSource.class);
            assertNotNull(dataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            assertEquals("jdbc:postgresql://custom-db-host:5433/custom_db?sslmode=require", hikariDataSource.getJdbcUrl());
        }
    }

    @Test
    @DisplayName("Should allow DB_URL full override to take precedence")
    void shouldAllowDbUrlOverride() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BcmServerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "DB_URL=jdbc:postgresql://override-host:9999/override_db",
                        "DB_USERNAME=override_user",
                        "DB_PASSWORD=override_pass",
                        "bcm.database.verification.enabled=false",
                        "SPRING_FLYWAY_ENABLED=false"
                )
                .run()) {

            DataSource dataSource = context.getBean(DataSource.class);
            assertNotNull(dataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            assertEquals("jdbc:postgresql://override-host:9999/override_db", hikariDataSource.getJdbcUrl());
            assertEquals("override_user", hikariDataSource.getUsername());
            assertEquals("override_pass", hikariDataSource.getPassword());
        }
    }

    @Test
    @DisplayName("Should default spring.flyway.enabled to true")
    void shouldDefaultFlywayEnabledToTrue() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .run()) {

            assertEquals("true", context.getEnvironment().getProperty("spring.flyway.enabled"));
        }
    }

    @org.springframework.context.annotation.Configuration
    static class EmptyConfig {}
}
