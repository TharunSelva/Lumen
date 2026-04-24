package com.energyapp.service;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.EnergyRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationTopSectionServiceTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;

    @BeforeEach
    void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
            insertHomeProfile(conn, 18.50);
            insertHomeAssets(conn, true, true, 4.8);
        }
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void build_usesLiveRecommendationAndForecastData() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-01-10 20:30:00", "2026-01-10 21:00:00", 18.0, 120.0, 0.0, 46.0, "discharge");
            insertForecastWindow(conn, "2026-01-10 22:30:00", "2026-01-10 23:00:00", 12.5, 85.0, 0.0, 40.0, "charge");
            insertForecastWindow(conn, "2026-01-11 11:30:00", "2026-01-11 12:00:00", 15.0, 70.0, 320.0, 63.0, "charge");
            insertForecastWindow(conn, "2026-01-11 12:00:00", "2026-01-11 12:30:00", 14.0, 65.0, 520.0, 78.0, "charge");
        }

        RecommendationTopSectionService.TopSectionsSnapshot snapshot =
                new RecommendationTopSectionService().build(record(
                        LocalDateTime.of(2026, 1, 10, 19, 0),
                        32.0,
                        90.0,
                        0.3,
                        50,
                        0.5
                ));

        assertEquals("Delay non-essential energy use.", snapshot.priorityCard().title());
        assertTrue(snapshot.priorityCard().why().contains("32.0p/kWh"));
        assertTrue(snapshot.priorityCard().impact().contains("\u00A318.50"));
        assertTrue(snapshot.tonightSection().items().stream()
                .anyMatch(item -> item.contains("12.5p/kWh at 22:30")));
        assertTrue(snapshot.tomorrowSection().items().stream()
                .anyMatch(item -> item.contains("520 W/m2")));
    }

    @Test
    void build_fallsBackToTomorrowSolarWhenNoLiveAlertIsActive() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-01-11 12:00:00", "2026-01-11 12:30:00", 14.0, 68.0, 510.0, 76.0, "charge");
        }

        RecommendationTopSectionService.TopSectionsSnapshot snapshot =
                new RecommendationTopSectionService().build(record(
                        LocalDateTime.of(2026, 1, 10, 19, 0),
                        14.0,
                        80.0,
                        0.4,
                        82,
                        0.7
                ));

        assertEquals("Tomorrow has a strong solar window", snapshot.priorityCard().title());
        assertEquals(RecommendationTopSectionService.NavigationTarget.FORECASTS, snapshot.priorityCard().navigationTarget());
        assertTrue(snapshot.priorityCard().summary().contains("12:00"));
        assertTrue(snapshot.tomorrowSection().items().stream()
                .anyMatch(item -> item.contains("510 W/m2")));
    }

    @Test
    void build_usesGracefulFallbacksWhenForecastCacheIsEmpty() {
        RecommendationTopSectionService.TopSectionsSnapshot snapshot =
                new RecommendationTopSectionService().build(record(
                        LocalDateTime.of(2026, 1, 10, 19, 0),
                        14.0,
                        92.0,
                        0.2,
                        81,
                        0.6
                ));

        assertEquals("Your home looks steady right now", snapshot.priorityCard().title());
        assertTrue(snapshot.tonightSection().items().stream()
                .anyMatch(item -> item.contains("14.0p/kWh")));
        assertTrue(snapshot.tomorrowSection().items().stream()
                .anyMatch(item -> item.contains("forecast is still refreshing")));
    }

    @Test
    void build_usesCachedForecastDayWhenSimulationDateIsOlder() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-04-24 22:00:00", "2026-04-24 22:30:00", 10.0, 82.0, 0.0, 42.0, "charge");
            insertForecastWindow(conn, "2026-04-25 12:00:00", "2026-04-25 12:30:00", 14.0, 61.0, 480.0, 79.0, "charge");
        }

        RecommendationTopSectionService.TopSectionsSnapshot snapshot =
                new RecommendationTopSectionService().build(record(
                        LocalDateTime.of(2026, 2, 1, 2, 30),
                        4.9,
                        78.0,
                        0.0,
                        64,
                        0.5
                ));

        assertTrue(snapshot.tomorrowSection().items().stream()
                .anyMatch(item -> item.contains("480 W/m2")));
    }

    private void insertHomeProfile(Connection conn, Double weeklyBudget) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO home_profile(home_id, first_name, timezone, weekly_budget_gbp, created_at)
                VALUES(?,?,?,?,datetime('now'))
                """)) {
            ps.setString(1, "HOME_1");
            ps.setString(2, "Richard");
            ps.setString(3, "Europe/London");
            if (weeklyBudget == null) {
                ps.setNull(4, java.sql.Types.REAL);
            } else {
                ps.setDouble(4, weeklyBudget);
            }
            ps.executeUpdate();
        }
    }

    private void insertHomeAssets(Connection conn, boolean hasSolar, boolean hasBattery, double solarCapacityKw) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO home_energy_assets(
                    home_id, has_ev, has_solar, has_battery, solar_capacity_kw, battery_capacity_kwh, min_soc_pct, max_discharge_kw
                ) VALUES(?,?,?,?,?,?,?,?)
                """)) {
            ps.setString(1, "HOME_1");
            ps.setInt(2, 1);
            ps.setInt(3, hasSolar ? 1 : 0);
            ps.setInt(4, hasBattery ? 1 : 0);
            ps.setDouble(5, solarCapacityKw);
            ps.setDouble(6, 10.5);
            ps.setInt(7, 20);
            ps.setDouble(8, 5.0);
            ps.executeUpdate();
        }
    }

    private void insertForecastWindow(
            Connection conn,
            String start,
            String end,
            Double tariff,
            Double carbon,
            Double solar,
            Double batterySoc,
            String batteryAction
    ) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO forecast_interval_cache(
                    home_id, ts_start, ts_end, tariff_p_per_kwh, carbon_gco2_kwh,
                    solar_shortwave_wm2, battery_soc_pct_pred, battery_action, retrieved_at
                ) VALUES(?,?,?,?,?,?,?,?,datetime('now'))
                """)) {
            ps.setString(1, "HOME_1");
            ps.setString(2, start);
            ps.setString(3, end);
            if (tariff == null) {
                ps.setNull(4, java.sql.Types.REAL);
            } else {
                ps.setDouble(4, tariff);
            }
            if (carbon == null) {
                ps.setNull(5, java.sql.Types.REAL);
            } else {
                ps.setDouble(5, carbon);
            }
            if (solar == null) {
                ps.setNull(6, java.sql.Types.REAL);
            } else {
                ps.setDouble(6, solar);
            }
            if (batterySoc == null) {
                ps.setNull(7, java.sql.Types.REAL);
            } else {
                ps.setDouble(7, batterySoc);
            }
            ps.setString(8, batteryAction);
            ps.executeUpdate();
        }
    }

    private EnergyRecord record(
            LocalDateTime timestamp,
            double pricePerKwh,
            double carbon,
            double solarGenKwh,
            int batteryLevel,
            double totalLoadKwh
    ) {
        return new EnergyRecord(
                timestamp,
                timestamp.toLocalTime().toString(),
                0.8,
                pricePerKwh,
                carbon,
                totalLoadKwh,
                solarGenKwh,
                batteryLevel,
                0.0,
                0.0,
                0.5,
                0.2
        );
    }
}
