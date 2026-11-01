package com.basketapp.bcmserver.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Component
@Order(1)
public class DatabaseConnectionVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionVerifier.class);

    private final DataSource dataSource;
    private final boolean enabled;

    public DatabaseConnectionVerifier(
            DataSource dataSource,
            @Value("${bcm.database.verification.enabled:true}") boolean enabled
    ) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    public DatabaseVerificationResult verifyConnection() {
        long startTime = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null) {
                long latencyMs = System.currentTimeMillis() - startTime;
                return DatabaseVerificationResult.failure(
                        VerificationStatus.FAILED,
                        "Database connection check failed: DataSource returned null connection",
                        latencyMs
                );
            }

            boolean isValid = connection.isValid(2);
            long latencyMs = System.currentTimeMillis() - startTime;

            if (isValid) {
                DatabaseMetaData metaData = connection.getMetaData();
                String productName = metaData.getDatabaseProductName();
                String productVersion = metaData.getDatabaseProductVersion();
                return DatabaseVerificationResult.success(productName, productVersion, latencyMs);
            } else {
                return DatabaseVerificationResult.failure(
                        VerificationStatus.FAILED,
                        "Database connection validation query check failed (isValid returned false)",
                        latencyMs
                );
            }
        } catch (SQLException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return DatabaseVerificationResult.failure(
                    VerificationStatus.UNREACHABLE,
                    e.getMessage(),
                    latencyMs
            );
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Database connectivity verification runner is disabled by configuration");
            return;
        }

        DatabaseVerificationResult result = verifyConnection();

        if (result.status() == VerificationStatus.CONNECTED) {
            log.info("===================================================================");
            log.info("Database Connectivity Verified Successfully");
            log.info("Product Name    : {}", result.databaseProductName());
            log.info("Product Version : {}", result.databaseProductVersion());
            log.info("Connection Pool : BcmHikariPool (active)");
            log.info("Validation Time : {} ms", result.validationLatencyMs());
            log.info("===================================================================");
        } else {
            log.error("===================================================================");
            log.error("CRITICAL: Database Connectivity Verification FAILED");
            log.error("Status: {}", result.status());
            log.error("Reason: {}", result.errorMessage());
            log.error("Please verify PostgreSQL is running and environment variables are valid.");
            log.error("Terminating application bootstrap.");
            log.error("===================================================================");

            throw new IllegalStateException("Database connectivity verification failed: " + result.errorMessage());
        }
    }
}

