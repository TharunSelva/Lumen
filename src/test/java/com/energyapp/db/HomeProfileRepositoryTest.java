package com.energyapp.db;

import com.energyapp.model.Geocode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void saveProfile_persistsFirstNameCityAndLocation() {
        repository.saveProfile("HOME_1", "Ava", "Southampton", new Geocode(50.9097, -1.4044));

        HomeProfileRepository.HomeProfileSnapshot profile = repository.getProfile("HOME_1");
        assertNotNull(profile);
        assertEquals("Ava", profile.firstName());
        assertEquals("Southampton", profile.cityName());
        assertNotNull(profile.location());
        assertEquals(50.9097, profile.location().latitude(), 0.0001);
        assertEquals(-1.4044, profile.location().longitude(), 0.0001);
    }

    @Test
    void updateHomeCity_replacesSavedCityWithoutChangingFirstName() {
        repository.saveProfile("HOME_1", "Ava", "Southampton", new Geocode(50.9097, -1.4044));

        repository.updateHomeCity("HOME_1", "Portsmouth", new Geocode(50.8198, -1.0880));

        assertEquals("Ava", repository.getFirstName("HOME_1"));
        assertEquals("Portsmouth", repository.getCityName("HOME_1"));
        assertEquals(50.8198, repository.getHomeLocation("HOME_1").latitude(), 0.0001);
        assertEquals(-1.0880, repository.getHomeLocation("HOME_1").longitude(), 0.0001);
    }
}
