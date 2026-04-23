package com.energyapp.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HomeProfileRepositoryTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private HomeProfileRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO home_profile(home_id, first_name, timezone, created_at) VALUES(?,?,?,datetime('now'))")) {
                stmt.setString(1, "HOME_1");
                stmt.setString(2, "Richard");
                stmt.setString(3, "Europe/London");
                stmt.executeUpdate();
            }
        }

        repository = new HomeProfileRepository();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void setWeeklyBudgetGbp_persistsAndReadsBackValue() {
        repository.setWeeklyBudgetGbp("HOME_1", 12.34);

        assertEquals(12.34, repository.getWeeklyBudgetGbp("HOME_1"), 0.0001);
    }

    @Test
    void setWeeklyBudgetGbp_writesAndReadsSqlNull() {
        repository.setWeeklyBudgetGbp("HOME_1", 9.99);
        repository.setWeeklyBudgetGbp("HOME_1", null);

        assertNull(repository.getWeeklyBudgetGbp("HOME_1"));
    }

    @Test
    void setWeeklyBudgetGbp_throwsWhenHomeDoesNotExist() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> repository.setWeeklyBudgetGbp("HOME_MISSING", 4.50));

        assertEquals(true, error.getMessage().contains("HOME_MISSING"));
    }
}
