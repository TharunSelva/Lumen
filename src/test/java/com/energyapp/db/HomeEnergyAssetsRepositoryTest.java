package com.energyapp.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeEnergyAssetsRepositoryTest {

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
    void fetchByHomeId_readsSolarCapacityWhenColumnExists() throws Exception {
        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
            try (PreparedStatement home = conn.prepareStatement(
                    "INSERT INTO home_profile(home_id, first_name, timezone, created_at) VALUES(?,?,?,datetime('now'))")) {
                home.setString(1, "HOME_1");
                home.setString(2, "Richard");
                home.setString(3, "Europe/London");
                home.executeUpdate();
            }
            try (PreparedStatement assets = conn.prepareStatement("""
                    INSERT INTO home_energy_assets(
                        home_id, has_solar, has_battery, solar_capacity_kw, battery_capacity_kwh, min_soc_pct, max_discharge_kw
                    ) VALUES(?,?,?,?,?,?,?)
                    """)) {
                assets.setString(1, "HOME_1");
                assets.setInt(2, 1);
                assets.setInt(3, 1);
                assets.setDouble(4, 4.8);
                assets.setDouble(5, 10.5);
                assets.setInt(6, 20);
                assets.setDouble(7, 5.0);
                assets.executeUpdate();
            }
        }

        HomeEnergyAssetsRepository.HomeEnergyAssets assets =
                new HomeEnergyAssetsRepository().fetchByHomeId("HOME_1");

        assertTrue(assets.hasSolar());
        assertTrue(assets.hasBattery());
        assertEquals(4.8, assets.solarCapacityKw(), 0.0001);
        assertEquals(4.8, assets.solarCapacityOrDefault(4.0), 0.0001);
    }

    @Test
    void fetchByHomeId_returnsSafeDefaultsWhenHomeIsMissing() throws Exception {
        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
        }

        HomeEnergyAssetsRepository.HomeEnergyAssets assets =
                new HomeEnergyAssetsRepository().fetchByHomeId("MISSING");

        assertFalse(assets.hasSolar());
        assertFalse(assets.hasBattery());
        assertEquals(4.0, assets.solarCapacityOrDefault(4.0), 0.0001);
    }
}
