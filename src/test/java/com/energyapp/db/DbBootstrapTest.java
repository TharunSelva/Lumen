package com.energyapp.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void ensureCityNameColumn_addsMissingTextColumnSafely() throws Exception {
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

        Method migration = DbBootstrap.class.getDeclaredMethod("ensureCityNameColumn", Connection.class);
        migration.setAccessible(true);

        try (Connection conn = Sqlite.openConnection()) {
            migration.invoke(null, conn);
            migration.invoke(null, conn);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(home_profile)")) {
                boolean found = false;
                while (rs.next()) {
                    if ("city_name".equals(rs.getString("name"))) {
                        found = true;
                        assertEquals("TEXT", rs.getString("type"));
                    }
                }
                assertTrue(found, "Migration should add city_name to home_profile");
            }
        }
    }

    @Test
    void ensureSolarCapacityColumn_addsMissingColumnAndSeedsHomeOneSafely() throws Exception {
        DbPaths.ensureDataDirExists();
        String url = "jdbc:sqlite:" + DbPaths.dbFile().toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE home_energy_assets (
                    home_id TEXT PRIMARY KEY,
                    has_ev INTEGER NOT NULL DEFAULT 0,
                    has_solar INTEGER NOT NULL DEFAULT 0,
                    has_battery INTEGER NOT NULL DEFAULT 0,
                    battery_capacity_kwh REAL,
                    min_soc_pct INTEGER,
                    max_discharge_kw REAL
                )
                """);
            stmt.execute("""
                INSERT INTO home_energy_assets(home_id, has_ev, has_solar, has_battery, battery_capacity_kwh, min_soc_pct, max_discharge_kw)
                VALUES('HOME_1', 1, 1, 1, 10.5, 20, 5.0)
                """);
        }

        Method migration = DbBootstrap.class.getDeclaredMethod("ensureSolarCapacityColumn", Connection.class);
        migration.setAccessible(true);

        try (Connection conn = Sqlite.openConnection()) {
            migration.invoke(null, conn);
            migration.invoke(null, conn);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(home_energy_assets)")) {
                boolean found = false;
                while (rs.next()) {
                    if ("solar_capacity_kw".equals(rs.getString("name"))) {
                        found = true;
                        assertEquals("REAL", rs.getString("type"));
                    }
                }
                assertTrue(found, "Migration should add solar_capacity_kw to home_energy_assets");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT solar_capacity_kw FROM home_energy_assets WHERE home_id = 'HOME_1'");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(4.0, rs.getDouble("solar_capacity_kw"), 0.0001);
            }
        }
    }

    @Test
    void ensureDeviceTableColumns_addsPhaseOneCanonicalColumnsSafely() throws Exception {
        DbPaths.ensureDataDirExists();
        String url = "jdbc:sqlite:" + DbPaths.dbFile().toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE device (
                    device_id TEXT PRIMARY KEY,
                    home_id TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    icon_key TEXT,
                    typical_power_w REAL,
                    typical_cycle_kwh REAL,
                    typical_duration_mins INTEGER,
                    power_state TEXT,
                    status TEXT,
                    current_draw_w REAL,
                    green_energy_only INTEGER NOT NULL DEFAULT 0,
                    ready_by_time TEXT,
                    policy TEXT,
                    next_run_window_start TEXT,
                    next_run_window_end TEXT,
                    next_run_est_cost_gbp REAL,
                    next_run_est_co2_kg REAL,
                    next_run_source_solar_pct REAL,
                    next_run_source_grid_pct REAL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
        }

        Method migration = DbBootstrap.class.getDeclaredMethod("ensureDeviceTableColumns", Connection.class);
        migration.setAccessible(true);

        try (Connection conn = Sqlite.openConnection()) {
            migration.invoke(null, conn);
            migration.invoke(null, conn);

            Map<String, String> columns = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(device)")) {
                while (rs.next()) {
                    columns.put(rs.getString("name"), rs.getString("type"));
                }
            }

            assertEquals("TEXT", columns.get("device_type"));
            assertEquals("TEXT", columns.get("schedule_time"));
            assertEquals("REAL", columns.get("price_cutoff_p_per_kwh"));
            assertEquals("TEXT", columns.get("active_run_until"));
            assertEquals("TEXT", columns.get("run_mode"));
            assertEquals("TEXT", columns.get("last_run_started_at"));
            assertEquals("TEXT", columns.get("last_run_ended_at"));
            assertEquals("TEXT", columns.get("last_scheduled_run_date"));
            assertEquals("INTEGER", columns.get("skip_next_run"));
            assertEquals("TEXT", columns.get("location_label"));
            assertEquals("TEXT", columns.get("deleted_at"));
        }
    }

    @Test
    void migrateUiDevicesToCanonicalDevices_isIdempotentAndPreservesLegacyFields() throws Exception {
        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");

            try (PreparedStatement homeInsert = conn.prepareStatement("""
                    INSERT INTO home_profile(home_id, first_name, timezone, created_at)
                    VALUES('HOME_1', 'Richard', 'Europe/London', datetime('now'))
                    """)) {
                homeInsert.executeUpdate();
            }

            try (PreparedStatement legacyInsert = conn.prepareStatement("""
                    INSERT INTO ui_device(home_id, device_name, device_type, price_cutoff, schedule_time, is_on, created_at, updated_at)
                    VALUES(?,?,?,?,?,?,?,?)
                    """)) {
                legacyInsert.setString(1, "HOME_1");
                legacyInsert.setString(2, "Living Room Plug");
                legacyInsert.setString(3, "SMART_PLUG");
                legacyInsert.setDouble(4, 19.5);
                legacyInsert.setString(5, null);
                legacyInsert.setInt(6, 1);
                legacyInsert.setString(7, "2026-04-20 08:00:00");
                legacyInsert.setString(8, "2026-04-20 09:00:00");
                legacyInsert.executeUpdate();

                legacyInsert.setString(1, "HOME_1");
                legacyInsert.setString(2, "Laundry");
                legacyInsert.setString(3, "WASHING_MACHINE");
                legacyInsert.setNull(4, java.sql.Types.REAL);
                legacyInsert.setString(5, "18:45");
                legacyInsert.setInt(6, 0);
                legacyInsert.setString(7, "2026-04-21 08:00:00");
                legacyInsert.setString(8, "2026-04-21 09:00:00");
                legacyInsert.executeUpdate();
            }

            Method migration = DbBootstrap.class.getDeclaredMethod("migrateUiDevicesToCanonicalDevices", Connection.class);
            migration.setAccessible(true);
            migration.invoke(null, conn);
            migration.invoke(null, conn);

            try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM device");
                 ResultSet countRs = countPs.executeQuery()) {
                assertTrue(countRs.next());
                assertEquals(2, countRs.getInt(1));
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT device_id, display_name, device_type, category, power_state, status,
                           schedule_time, price_cutoff_p_per_kwh, created_at, updated_at,
                           active_run_until, run_mode, deleted_at
                    FROM device
                    WHERE device_id = ?
                    """)) {
                ps.setString(1, "USR_UI_1");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Living Room Plug", rs.getString("display_name"));
                    assertEquals("SMART_PLUG", rs.getString("device_type"));
                    assertEquals("SMART_PLUG", rs.getString("category"));
                    assertEquals("ON", rs.getString("power_state"));
                    assertEquals("IDLE", rs.getString("status"));
                    assertEquals(19.5, rs.getDouble("price_cutoff_p_per_kwh"), 0.0001);
                    assertEquals("2026-04-20 08:00:00", rs.getString("created_at"));
                    assertEquals("2026-04-20 09:00:00", rs.getString("updated_at"));
                    assertEquals(null, rs.getString("schedule_time"));
                    assertEquals(null, rs.getString("active_run_until"));
                    assertEquals(null, rs.getString("run_mode"));
                    assertEquals(null, rs.getString("deleted_at"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT device_id, display_name, device_type, category, power_state, status,
                           schedule_time, price_cutoff_p_per_kwh
                    FROM device
                    WHERE device_id = ?
                    """)) {
                ps.setString(1, "USR_UI_2");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Laundry", rs.getString("display_name"));
                    assertEquals("WASHING_MACHINE", rs.getString("device_type"));
                    assertEquals("LAUNDRY", rs.getString("category"));
                    assertEquals("OFF", rs.getString("power_state"));
                    assertEquals("OFF", rs.getString("status"));
                    assertEquals("18:45", rs.getString("schedule_time"));
                    assertEquals(0.0, rs.getDouble("price_cutoff_p_per_kwh"), 0.0001);
                    assertTrue(rs.wasNull());
                }
            }
        }
    }

    @Test
    void migrateUiDevicesToCanonicalDevices_doesNotRecreateSoftDeletedCanonicalRows() throws Exception {
        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");

            try (PreparedStatement homeInsert = conn.prepareStatement("""
                    INSERT INTO home_profile(home_id, first_name, timezone, created_at)
                    VALUES('HOME_1', 'Richard', 'Europe/London', datetime('now'))
                    """)) {
                homeInsert.executeUpdate();
            }

            try (PreparedStatement legacyInsert = conn.prepareStatement("""
                    INSERT INTO ui_device(home_id, device_name, device_type, price_cutoff, schedule_time, is_on, created_at, updated_at)
                    VALUES('HOME_1', 'Kitchen Plug', 'SMART_PLUG', 18.0, NULL, 1, '2026-04-22 07:00:00', '2026-04-22 08:00:00')
                    """)) {
                legacyInsert.executeUpdate();
            }

            try (PreparedStatement deletedInsert = conn.prepareStatement("""
                    INSERT INTO device(
                      device_id, home_id, display_name, device_type, category, icon_key,
                      typical_power_w, typical_cycle_kwh, typical_duration_mins,
                      power_state, status, current_draw_w,
                      green_energy_only, ready_by_time, schedule_time, price_cutoff_p_per_kwh, policy,
                      next_run_window_start, next_run_window_end,
                      next_run_est_cost_gbp, next_run_est_co2_kg,
                      next_run_source_solar_pct, next_run_source_grid_pct,
                      active_run_until, run_mode, last_run_started_at, last_run_ended_at,
                      last_scheduled_run_date, skip_next_run, location_label,
                      created_at, updated_at, deleted_at
                    ) VALUES(
                      'USR_UI_1', 'HOME_1', 'Kitchen Plug', 'SMART_PLUG', 'SMART_PLUG', NULL,
                      NULL, NULL, NULL,
                      'OFF', 'OFF', NULL,
                      0, NULL, NULL, 18.0, NULL,
                      NULL, NULL,
                      NULL, NULL,
                      NULL, NULL,
                      NULL, NULL, NULL, NULL,
                      NULL, 0, NULL,
                      '2026-04-22 07:00:00', '2026-04-22 08:00:00', '2026-04-22 09:00:00'
                    )
                    """)) {
                deletedInsert.executeUpdate();
            }

            Method migration = DbBootstrap.class.getDeclaredMethod("migrateUiDevicesToCanonicalDevices", Connection.class);
            migration.setAccessible(true);
            migration.invoke(null, conn);

            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) AS device_count, MAX(deleted_at) AS deleted_at
                    FROM device
                    WHERE home_id = 'HOME_1' AND lower(trim(display_name)) = 'kitchen plug'
                    """);
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("device_count"));
                assertNotNull(rs.getString("deleted_at"));
                assertFalse(rs.getString("deleted_at").isBlank());
            }
        }
    }
}
