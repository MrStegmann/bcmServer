package com.basketapp.bcmserver.infrastructure.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically loads key-value pairs from a local .env file into the Spring {@link ConfigurableEnvironment}.
 * <p>
 * Ensures that:
 * <ul>
 *   <li>System properties (-D) and OS environment variables take precedence over .env entries.</li>
 *   <li>.env entries take precedence over default placeholder fallbacks in application.yml.</li>
 *   <li>Explicit properties supplied programmatically (e.g., via {@code defaultProperties} in tests) are preserved.</li>
 *   <li>Dotenv loading can be explicitly disabled via {@code bcm.dotenv.enabled=false} or {@code dotenv.enabled=false}.</li>
 * </ul>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    public static final String PROPERTY_SOURCE_NAME = "dotenvProperties";
    public static final String DOTENV_ENABLED_PROPERTY = "bcm.dotenv.enabled";
    public static final String DOTENV_ENABLED_PROPERTY_ALT = "dotenv.enabled";

    // Run before ConfigDataEnvironmentPostProcessor (HIGHEST_PRECEDENCE + 10) so properties
    // from .env are available for placeholder resolution in application.yml and profile activation.
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 9;

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enabledVal = environment.getProperty(DOTENV_ENABLED_PROPERTY);
        if (enabledVal == null) {
            enabledVal = environment.getProperty(DOTENV_ENABLED_PROPERTY_ALT);
        }
        if ("false".equalsIgnoreCase(enabledVal)) {
            log.info("Dotenv loading disabled via configuration");
            return;
        }

        File envFile = resolveEnvFile();
        if (envFile == null || !envFile.exists()) {
            log.debug("No .env file found in working directory or user.dir");
            return;
        }

        try {
            File dir = envFile.getParentFile();
            Dotenv dotenv = Dotenv.configure()
                    .directory(dir != null ? dir.getAbsolutePath() : ".")
                    .filename(envFile.getName())
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> dotenvMap = new HashMap<>();
            PropertySource<?> defaultProps = environment.getPropertySources().get("defaultProperties");

            for (DotenvEntry entry : dotenv.entries()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // If explicitly set in defaultProperties (by test or builder), respect that override
                if (defaultProps != null && defaultProps.containsProperty(key)) {
                    continue;
                }

                dotenvMap.put(key, value);
            }

            if (!dotenvMap.isEmpty()) {
                MapPropertySource dotenvPropertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap);

                // Insert after SYSTEM_ENVIRONMENT so real OS environment variables win over .env
                if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                    environment.getPropertySources().addAfter(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            dotenvPropertySource
                    );
                } else {
                    environment.getPropertySources().addFirst(dotenvPropertySource);
                }

                log.info("Loaded {} configuration properties from {}", dotenvMap.size(), envFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Failed to load .env file from {}: {}", envFile.getAbsolutePath(), e.getMessage());
        }
    }

    private File resolveEnvFile() {
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            File userDirEnv = new File(userDir, ".env");
            if (userDirEnv.exists()) {
                return userDirEnv;
            }
            return null;
        }
        File direct = new File(".env");
        return direct.exists() ? direct : null;
    }
}
