package com.energyapp.db;

import com.energyapp.model.EnergyRecord;
import com.energyapp.utils.CsvReadForService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DbBootstrap {
    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DbBootstrap() {}

    public static void init() {
        DbPaths.ensureDataDirExists();

        try (Connection conn = Sqlite.openConnection()) {
            // 1) Always ensure schema exists
            SqlScriptRunner.runResource(conn, "/com/schema.sql");

            // 2) Seed only once
            if (!metaEquals(conn, "seed_applied", "true")) {
                SqlScriptRunner.runResource(conn, "/com/seed.sql");
                upsertMeta(conn, "seed_applied", "true");
            }

            // 3) Import simulation CSV into DB only if empty
            if (tableIsEmpty(conn, "energy_interval")) {
                importSimulationCsv(conn);
                upsertMeta(conn, "csv_imported", "true");
            }

            // 4) (Optional but recommended) Cache current carbon intensity + generation mix once
            // If offline, this will just fail quietly and your app still runs with simulated/carbon from CSV.
            try {
                cacheCurrentCarbonAndMix(conn);
                upsertMeta(conn, "last_carbon_cache_at", LocalDateTime.now().format(TS_FMT));
            } catch (Exception ignored) {
                // Keep app usable offline
            }

        } catch (Exception e) {
            throw new RuntimeException("DB bootstrap failed", e);
        }
    }

    // ------------------------
    // CSV -> DB import
    // ------------------------
    private static void importSimulationCsv(Connection conn) throws Exception {
        InputStream csv = DbBootstrap.class.getResourceAsStream("/com/simulation_data.csv");
        if (csv == null) throw new IllegalStateException("Missing /com/simulation_data.csv");

        List<EnergyRecord> records = CsvReadForService.loadData(csv);

        conn.setAutoCommit(false);
        try {
            // Insert into energy_interval
            String insEnergy = """
                INSERT OR IGNORE INTO energy_interval(
                  home_id, ts_start, time_label, price_p_per_kwh, carbon_gco2_kwh,
                  total_load_kwh, solar_gen_kwh, battery_level_pct, grid_import_kwh, data_source
                ) VALUES(?,?,?,?,?,?,?,?,?,?)
                """;

            // Insert per-device usage
            String insDev = """
                INSERT OR IGNORE INTO device_usage_interval(home_id, ts_start, device_id, kwh)
                VALUES(?,?,?,?)
                """;

            try (PreparedStatement pe = conn.prepareStatement(insEnergy);
                 PreparedStatement pd = conn.prepareStatement(insDev)) {

                for (EnergyRecord r : records) {
                    String ts = r.getTimestamp().format(TS_FMT);

                    pe.setString(1, HOME_ID);
                    pe.setString(2, ts);
                    pe.setString(3, r.getTimeLabel());               // requires small EnergyRecord change (below)
                    pe.setDouble(4, r.getPricePerKwh());
                    pe.setDouble(5, r.getCarbon());
                    pe.setDouble(6, r.getTotalLoadKwh());
                    pe.setDouble(7, r.getSolarGenKwh());
                    pe.setInt(8, r.getBatteryLevel());
                    pe.setDouble(9, r.getGridImport());
                    pe.setString(10, "SEED");
                    pe.addBatch();

                    // Map CSV device columns to seeded device IDs
                    insertDev(pd, ts, "DEV_EV_1",   r.getEvChargerUsageKwh());
                    insertDev(pd, ts, "DEV_WM_1",   r.getWashingMachineUsageKwh());
                    insertDev(pd, ts, "DEV_HEAT_1", r.getHeatingUsageKwh());
                    insertDev(pd, ts, "DEV_BASE_1", r.getBaseloadKwh());
                }

                pe.executeBatch();
                pd.executeBatch();
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void insertDev(PreparedStatement pd, String ts, String devId, double kwh) throws SQLException {
        pd.setString(1, HOME_ID);
        pd.setString(2, ts);
        pd.setString(3, devId);
        pd.setDouble(4, kwh);
        pd.addBatch();
    }

    // ------------------------
    // Carbon Intensity API cache (national)
    // ------------------------
    private static void cacheCurrentCarbonAndMix(Connection conn) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

        // Current intensity
        HttpRequest intensityReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.carbonintensity.org.uk/intensity"))
                .GET()
                .build();

        String intensityJson = client.send(intensityReq, HttpResponse.BodyHandlers.ofString()).body();
        CarbonIntensityResponse intensity = gson.fromJson(intensityJson, CarbonIntensityResponse.class);

        if (intensity != null && intensity.data != null && !intensity.data.isEmpty()) {
            CarbonIntensityResponse.IntensityData d = intensity.data.get(0);

            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO forecast_interval_cache(
                  home_id, ts_start, ts_end, carbon_gco2_kwh, retrieved_at, source_json
                ) VALUES(?,?,?,?,?,?)
                """)) {
                // Store carbon in forecast cache too (so Forecasts page has something immediately)
                ps.setString(1, HOME_ID);
                ps.setString(2, toAppTs(d.from));
                ps.setString(3, toAppTs(d.to));
                ps.setDouble(4, d.intensity != null && d.intensity.actual != null ? d.intensity.actual : d.intensity.forecast);
                ps.setString(5, LocalDateTime.now().format(TS_FMT));
                ps.setString(6, "{\"source\":\"carbonintensity\",\"endpoint\":\"/intensity\"}");
                ps.executeUpdate();
            }
        }

        // Current generation mix - with normalised rows
        HttpRequest genReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.carbonintensity.org.uk/generation"))
                .GET()
                .build();

        String genJson = client.send(genReq, HttpResponse.BodyHandlers.ofString()).body();

        // 1) Insert the current window (data object)
        upsertGenerationMixRows(conn, genJson, "national", null);

        // 2) Backfill last 24h (so the  “last 4 hours” UI works immediately)
        // The API docs list /generation/{from}/pt24h. :contentReference[oaicite:1]{index=1}
        String fromForPt24h = extractGenerationToTimestamp(genJson); // e.g. "2026-02-27T00:30Z"
        if (fromForPt24h != null) {
            String encoded = URLEncoder.encode(fromForPt24h, StandardCharsets.UTF_8);
            String ptUrl = "https://api.carbonintensity.org.uk/generation/" + encoded + "/pt24h";

            HttpRequest ptReq = HttpRequest.newBuilder().uri(URI.create(ptUrl)).GET().build();
            String ptJson = client.send(ptReq, HttpResponse.BodyHandlers.ofString()).body();

            // pt24h is typically returned as an array of windows (data array)
            upsertGenerationMixRows(conn, ptJson, "national", null);
        }

        // 3) Cleanup (toavoid DB growing forever)
        cleanupOldGenerationMix(conn, "national", null, 72);
    }

    private static String toAppTs(String iso) {
        // Carbon Intensity returns ISO timestamps; we store app timestamps in "yyyy-MM-dd HH:mm:ss"
        // Minimal conversion: keep first 19 chars and replace 'T' with space.
        if (iso == null || iso.length() < 19) return iso;
        return iso.substring(0, 19).replace('T', ' ');
    }

    // ------------------------
    // Meta helpers
    // ------------------------
    private static boolean tableIsEmpty(Connection conn, String table) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM " + table)) {
            return rs.next() && rs.getLong("c") == 0;
        }
    }

    private static boolean metaEquals(Connection conn, String key, String expected) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT meta_value FROM app_meta WHERE meta_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return expected.equals(rs.getString(1));
            }
        }
    }

    private static void upsertMeta(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO app_meta(meta_key, meta_value) VALUES(?,?)
            ON CONFLICT(meta_key) DO UPDATE SET meta_value=excluded.meta_value
            """)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    // ------------------------
    // DTOs (Gson)
    // ------------------------
    private static final class CarbonIntensityResponse {
        java.util.List<IntensityData> data;

        static final class IntensityData {
            String from;
            String to;
            Intensity intensity;
        }
        static final class Intensity {
            Integer forecast;
            Integer actual;
        }
    }

    private static final class GenerationMixResponse {
        GenData data;

        static final class GenData {
            String from;
            String to;
            java.util.List<Fuel> generationmix;
        }
        static final class Fuel {
            String fuel;
            Double perc;
        }
    }
}