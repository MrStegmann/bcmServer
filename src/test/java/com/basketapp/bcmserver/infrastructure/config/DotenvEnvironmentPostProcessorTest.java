package com.basketapp.bcmserver.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotenvEnvironmentPostProcessorTest {

    private final DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();

    @Test
    @DisplayName("Should have order HIGHEST_PRECEDENCE + 9 before ConfigDataEnvironmentPostProcessor")
    void shouldHaveExpectedOrder() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 9, processor.getOrder());
    }

    @Test
    @DisplayName("Should load properties from .env into environment property sources")
    void shouldLoadPropertiesFromEnvFile(@TempDir Path tempDir) throws IOException {
        Path envPath = tempDir.resolve(".env");
        Files.writeString(envPath, "TEST_DOTENV_KEY=custom_val_123\nTEST_DOTENV_PORT=9090\n");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            ConfigurableEnvironment environment = new StandardEnvironment();
            processor.postProcessEnvironment(environment, new SpringApplication());

            assertNotNull(environment.getPropertySources().get(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
            assertEquals("custom_val_123", environment.getProperty("TEST_DOTENV_KEY"));
            assertEquals("9090", environment.getProperty("TEST_DOTENV_PORT"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("Should skip loading when bcm.dotenv.enabled is false")
    void shouldSkipWhenBcmDotenvDisabled(@TempDir Path tempDir) throws IOException {
        Path envPath = tempDir.resolve(".env");
        Files.writeString(envPath, "TEST_DOTENV_KEY=should_not_load\n");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(
                    new MapPropertySource("testOverrides", Map.of(DotenvEnvironmentPostProcessor.DOTENV_ENABLED_PROPERTY, "false"))
            );

            processor.postProcessEnvironment(environment, new SpringApplication());

            assertNull(environment.getPropertySources().get(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
            assertNull(environment.getProperty("TEST_DOTENV_KEY"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("Should skip loading when dotenv.enabled is false")
    void shouldSkipWhenDotenvDisabled(@TempDir Path tempDir) throws IOException {
        Path envPath = tempDir.resolve(".env");
        Files.writeString(envPath, "TEST_DOTENV_KEY=should_not_load\n");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(
                    new MapPropertySource("testOverrides", Map.of(DotenvEnvironmentPostProcessor.DOTENV_ENABLED_PROPERTY_ALT, "false"))
            );

            processor.postProcessEnvironment(environment, new SpringApplication());

            assertNull(environment.getPropertySources().get(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
            assertNull(environment.getProperty("TEST_DOTENV_KEY"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("Should preserve explicit values from defaultProperties over .env")
    void shouldPreserveDefaultPropertiesOverrides(@TempDir Path tempDir) throws IOException {
        Path envPath = tempDir.resolve(".env");
        Files.writeString(envPath, "DB_HOST=from_dotenv\nOTHER_KEY=from_dotenv\n");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addLast(
                    new MapPropertySource("defaultProperties", Map.of("DB_HOST", "explicit_default"))
            );

            processor.postProcessEnvironment(environment, new SpringApplication());

            assertEquals("explicit_default", environment.getProperty("DB_HOST"));
            assertEquals("from_dotenv", environment.getProperty("OTHER_KEY"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("Should handle missing .env gracefully without error")
    void shouldHandleMissingEnvGracefully(@TempDir Path tempDir) {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            ConfigurableEnvironment environment = new StandardEnvironment();
            assertDoesNotThrow(() -> processor.postProcessEnvironment(environment, new SpringApplication()));
            assertNull(environment.getPropertySources().get(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("Should be discoverable via SpringFactoriesLoader under EnvironmentPostProcessor")
    void shouldBeDiscoverableViaSpringFactoriesLoader() {
        List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class,
                getClass().getClassLoader()
        );

        assertTrue(
                factoryNames.contains(DotenvEnvironmentPostProcessor.class.getName()),
                "DotenvEnvironmentPostProcessor should be registered in META-INF/spring.factories"
        );
    }

    @Test
    @DisplayName("Should load .env when bootstrapped via SpringApplication SPI")
    void shouldLoadEnvWhenBootstrappedViaSpringApplication(@TempDir Path tempDir) throws IOException {
        Path envPath = tempDir.resolve(".env");
        Files.writeString(envPath, "BOOTSTRAP_SPI_KEY=discovered_via_spring_factories\n");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            SpringApplication app = new SpringApplicationBuilder(DummyTestConfig.class)
                    .web(WebApplicationType.NONE)
                    .build();

            try (ConfigurableApplicationContext context = app.run()) {
                assertEquals("discovered_via_spring_factories", context.getEnvironment().getProperty("BOOTSTRAP_SPI_KEY"));
            }
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Configuration
    static class DummyTestConfig {
    }
}

