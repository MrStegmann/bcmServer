package com.basketapp.bcmserver.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SupabaseDirectConnectivityTest {

    private static final String SUPABASE_DIRECT_HOST = "db.ccthgmjmkdxhpqovbbvk.supabase.co";
    private static final int SUPABASE_PORT = 5432;
    private static final String SUPABASE_USER = "postgres";
    private static final String SUPABASE_PASS = "nU4goTgNHwsvTrvV";
    private static final String SUPABASE_DB = "postgres";

    @Test
    @DisplayName("Verify direct TCP/JDBC connectivity to Supabase when IPv6 routing is present")
    void shouldConnectDirectlyToSupabaseWhenHostIsResolvable() {
        boolean hostResolvable = false;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(SUPABASE_DIRECT_HOST);
            hostResolvable = addresses != null && addresses.length > 0;
        } catch (UnknownHostException e) {
            hostResolvable = false;
        }

        assumeTrue(hostResolvable,
                "Supabase direct host " + SUPABASE_DIRECT_HOST + " is not resolvable. Skipping live connectivity check.");

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require",
                SUPABASE_DIRECT_HOST, SUPABASE_PORT, SUPABASE_DB);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, SUPABASE_USER, SUPABASE_PASS);
             Statement stmt = conn.createStatement()) {

            assertNotNull(conn, "Connection should be established");
            assertFalse(conn.isClosed(), "Connection should be open");
            assertTrue(conn.isValid(5), "Connection should be valid within 5 seconds");

            DatabaseMetaData metaData = conn.getMetaData();
            assertEqualsOrContains("PostgreSQL", metaData.getDatabaseProductName());

            Set<String> tables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }

            assertTrue(tables.contains("users"), "Expected 'users' table to exist in public schema");
            assertTrue(tables.contains("teams"), "Expected 'teams' table to exist in public schema");
            assertTrue(tables.contains("players"), "Expected 'players' table to exist in public schema");
            assertTrue(tables.contains("games"), "Expected 'games' table to exist in public schema");
            assertTrue(tables.contains("player_stats"), "Expected 'player_stats' table to exist in public schema");
            assertTrue(tables.contains("flyway_schema_history"), "Expected 'flyway_schema_history' table to exist");

        } catch (Exception e) {
            throw new AssertionError("Failed to connect or verify Supabase database", e);
        }
    }

    private void assertEqualsOrContains(String expected, String actual) {
        assertNotNull(actual, "Actual database product name must not be null");
        assertTrue(actual.toLowerCase().contains(expected.toLowerCase()),
                "Expected database product to contain '" + expected + "', got: " + actual);
    }
}
