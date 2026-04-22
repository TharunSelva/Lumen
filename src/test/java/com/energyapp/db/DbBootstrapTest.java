package com.energyapp.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbBootstrapTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void ensureWeeklyBudgetColumn_addsMissingRealColumnSafely() throws Exception {
        DbPaths.ensureDataDirExists();
        String url = "jdbc:sqlite:" + DbPaths.dbFile().toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE home_profile (
                    home_id TEXT PRIMARY KEY,
                    first_name TEXT NOT NULL,
                    timezone TEXT NOT NULL,
                    postcode_outcode TEXT,
                    lat REAL,
                    lon REAL,
                    created_at TEXT NOT NULL
                )
                """);
        }

        Method migration = DbBootstrap.class.getDeclaredMethod("ensureWeeklyBudgetColumn", Connection.class);
        migration.setAccessible(true);

        try (Connection conn = Sqlite.openConnection()) {
            migration.invoke(null, conn);
            migration.invoke(null, conn);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(home_profile)")) {
                boolean found = false;
                while (rs.next()) {
                    if ("weekly_budget_gbp".equals(rs.getString("name"))) {
                        found = true;
                        assertEquals("REAL", rs.getString("type"));
                    }
                }
                assertTrue(found, "Migration should add weekly_budget_gbp to home_profile");
            }
        }
    }
}
