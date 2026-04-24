package com.energyapp.service;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
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

class ForecastOverviewServiceTest {

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
            insertHomeProfile(conn);
            insertHomeAssets(conn);
        }
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void load_usesCachedForecastDayWhenPreferredReferenceHasNoMatches() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-04-24 22:00:00", 10.0, 84.0, 0.0, 38.0, "charge");
            insertForecastWindow(conn, "2026-04-25 12:00:00", 14.0, 61.0, 520.0, 79.0, "charge");
        }

        ForecastOverviewService service = new ForecastOverviewService();
        ForecastOverviewService.ForecastOverviewSnapshot snapshot =
                service.load(ForecastOverviewService.HOME_ID, LocalDateTime.of(2026, 2, 1, 2, 30));

        assertEquals(LocalDateTime.of(2026, 4, 24, 23, 0), snapshot.anchorTime());
        assertTrue(snapshot.tomorrowWindows().stream().anyMatch(window -> window.solarShortwaveWm2() != null));
    }

    @Test
    void buildTomorrowSummary_usesRealTariffSolarAndBatterySignals() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-04-24 22:00:00", 10.0, 84.0, 0.0, 38.0, "charge");
            insertForecastWindow(conn, "2026-04-25 01:00:00", 8.5, 72.0, 0.0, 44.0, "charge");
            insertForecastWindow(conn, "2026-04-25 12:00:00", 14.0, 61.0, 520.0, 79.0, "charge");
            insertForecastWindow(conn, "2026-04-25 18:00:00", 18.0, 49.0, 35.0, 42.0, "discharge");
        }

        ForecastOverviewService service = new ForecastOverviewService();
        ForecastOverviewService.TomorrowSummarySnapshot summary =
                service.buildTomorrowSummary(service.load(ForecastOverviewService.HOME_ID, null));

        assertTrue(summary.items().stream().anyMatch(item -> item.contains("8.5p/kWh")));
        assertTrue(summary.items().stream().anyMatch(item -> item.contains("520 W/m2")));
        assertTrue(summary.items().stream().anyMatch(item -> item.contains("charge")));
    }

    @Test
    void buildBatteryPanel_reportsTomorrowStorageRangeAndCapacity() throws Exception {
        try (Connection conn = Sqlite.openConnection()) {
            insertForecastWindow(conn, "2026-04-24 22:00:00", 10.0, 84.0, 0.0, 38.0, "charge");
            insertForecastWindow(conn, "2026-04-25 02:00:00", 8.5, 72.0, 0.0, 32.0, "charge");
            insertForecastWindow(conn, "2026-04-25 13:00:00", 14.0, 61.0, 520.0, 81.0, "charge");
            insertForecastWindow(conn, "2026-04-25 19:00:00", 18.0, 49.0, 20.0, 36.0, "discharge");
        }

        ForecastOverviewService service = new ForecastOverviewService();
        ForecastOverviewService.BatteryPanelSnapshot battery =
                service.buildBatteryPanel(service.load(ForecastOverviewService.HOME_ID, null));

        assertTrue(battery.headline().contains("81%"));
        assertTrue(battery.body().contains("32%"));
        assertTrue(battery.highlights().stream().anyMatch(item -> item.contains("10.5 kWh")));
        assertTrue(battery.highlights().stream().anyMatch(item -> item.contains("Discharge") || item.contains("discharge")));
    }

    private void insertHomeProfile(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO home_profile(home_id, first_name, timezone, weekly_budget_gbp, created_at)
                VALUES(?,?,?,?,datetime('now'))
                """)) {
            ps.setString(1, "HOME_1");
            ps.setString(2, "Richard");
            ps.setString(3, "Europe/London");
            ps.setDouble(4, 18.50);
            ps.executeUpdate();
        }
    }

    private void insertHomeAssets(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO home_energy_assets(
                    home_id, has_ev, has_solar, has_battery, solar_capacity_kw, battery_capacity_kwh, min_soc_pct, max_discharge_kw
                ) VALUES(?,?,?,?,?,?,?,?)
                """)) {
            ps.setString(1, "HOME_1");
            ps.setInt(2, 1);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setDouble(5, 4.8);
            ps.setDouble(6, 10.5);
            ps.setInt(7, 20);
            ps.setDouble(8, 5.0);
            ps.executeUpdate();
        }
    }

    private void insertForecastWindow(
            Connection conn,
            String start,
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
            ps.setString(3, start);
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
}
