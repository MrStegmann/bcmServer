package com.basketapp.bcmserver.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DatabaseConnectionVerifierTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    private DatabaseConnectionVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new DatabaseConnectionVerifier(dataSource, true);
    }

    @Test
    @DisplayName("Should successfully verify connection when database is available")
    void shouldSuccessfullyVerifyConnectionWhenAvailable() throws SQLException {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(true);
        given(connection.getMetaData()).willReturn(metaData);
        given(metaData.getDatabaseProductName()).willReturn("PostgreSQL");
        given(metaData.getDatabaseProductVersion()).willReturn("16.4 (Debian 16.4-1.pgdg120+1)");

        DatabaseVerificationResult result = verifier.verifyConnection();

        assertEquals(VerificationStatus.CONNECTED, result.status());
        assertEquals("PostgreSQL", result.databaseProductName());
        assertEquals("16.4 (Debian 16.4-1.pgdg120+1)", result.databaseProductVersion());
        assertNull(result.errorMessage());
        assertTrue(result.validationLatencyMs() >= 0);
    }

    @Test
    @DisplayName("Should return FAILED when connection isValid returns false")
    void shouldReturnFailedWhenConnectionIsNotValid() throws SQLException {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(false);

        DatabaseVerificationResult result = verifier.verifyConnection();

        assertEquals(VerificationStatus.FAILED, result.status());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("validation query check failed"));
    }

    @Test
    @DisplayName("Should return UNREACHABLE when SQLException occurs during connection attempt")
    void shouldReturnUnreachableWhenSQLExceptionThrown() throws SQLException {
        given(dataSource.getConnection()).willThrow(new SQLException("Connection refused to localhost:5432"));

        DatabaseVerificationResult result = verifier.verifyConnection();

        assertEquals(VerificationStatus.UNREACHABLE, result.status());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("Connection refused"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException on runner execution when verification fails")
    void shouldThrowExceptionOnRunnerExecutionWhenVerificationFails() throws SQLException {
        given(dataSource.getConnection()).willThrow(new SQLException("Connection refused"));
        ApplicationArguments mockArgs = mock(ApplicationArguments.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verifier.run(mockArgs)
        );

        assertTrue(exception.getMessage().contains("Database connectivity verification failed"));
    }

    @Test
    @DisplayName("Should succeed on runner execution when verification succeeds")
    void shouldSucceedOnRunnerExecutionWhenVerificationSucceeds() throws SQLException {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(true);
        given(connection.getMetaData()).willReturn(metaData);
        given(metaData.getDatabaseProductName()).willReturn("PostgreSQL");
        given(metaData.getDatabaseProductVersion()).willReturn("16.4");
        ApplicationArguments mockArgs = mock(ApplicationArguments.class);

        assertDoesNotThrow(() -> verifier.run(mockArgs));
    }
}

