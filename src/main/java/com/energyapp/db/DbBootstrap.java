package com.energyapp.db;

//import com.energyapp.model.EnergyRecord;
//import com.energyapp.utils.CsvReadForService;
import com.energyapp.service.OctopusEnergyTariffService;
import com.energyapp.db.OpenMeteoSolarService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class DbBootstrap {
    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DbBootstrap() {}
    private static final String TEMPLATE_DB_RESOURCE = "/com/energy_template_db.sqlite";
    private static final String APP_SCHEMA_VERSION = "1";
    private static final String TEMPLATE_DB_NAME = "energy_template_db";
    private static final String TEMPLATE_DB_VERSION = "1";

    public static void init() {
        DbPaths.ensureDataDirExists();
        System.out.println("DB PATH = " + DbPaths.dbFile().toAbsolutePath());

        try {
            // for debugging;
            boolean existedBeforeInstall = DbPaths.dbExists();
            System.out.println("DB PATH = " + DbPaths.dbFile().toAbsolutePath());
            System.out.println("DB already existed before install step? " + existedBeforeInstall);

            installTemplateDbIfMissing();

            System.out.println("DB exists after install step? " + DbPaths.dbExists());

            try (Connection conn = Sqlite.openConnection()) {
                // safety: keeps schema creation idempotent
                SqlScriptRunner.runResource(conn, "/com/schema.sql");
                ensureWeeklyBudgetColumn(conn);
                ensureVersionMetaIfMissing(conn);
                verifyInstalledDbVersion(conn);

                // Mark installed-from-template once
                upsertMeta(conn, "template_installed", "true");

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE home_profile SET lat = NULL, lon = NULL WHERE home_id = 'HOME_1'");
                }

                // Run API caching for current carbon and mix
                try {
                    cacheCurrentCarbonAndMix(conn);
                    upsertMeta(conn, "last_carbon_cache_at", LocalDateTime.now().format(TS_FMT));
                } catch (Exception e) {
                    System.err.println("API cache failed (carbon/mix).");
                    e.printStackTrace();
                }

                // Run API caching for Octopus API tariffs
                try {
                    cacheOctopusTariffs(conn);
                } catch (Exception e) {
                    System.err.println("API cache failed (octopus).");
                    e.printStackTrace();
                }

                // Run caching for MeteoSolar
                try {
                    cacheOpenMeteoSolar(conn);
                } catch (Exception e) {
                    System.err.println("API cache failed (open-meteo solar).");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("DB bootstrap failed", e);
        }
    }

    private static void ensureWeeklyBudgetColumn(Connection conn) throws SQLException {
        if (hasColumn(conn, "home_profile", "weekly_budget_gbp")) {
            return;
        }

        try (Statement statement = conn.createStatement()) {
            statement.execute("ALTER TABLE home_profile ADD COLUMN weekly_budget_gbp REAL");
        }
    }

    private static boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    // helper method to write schema/template versions and template name (metadata) into app_meta in the DB
    private static void ensureVersionMetaIfMissing(Connection conn) throws SQLException {
        if (getMeta(conn, "schema_version") == null) {
            upsertMeta(conn, "schema_version", APP_SCHEMA_VERSION);
        }
        if (getMeta(conn, "template_name") == null) {
            upsertMeta(conn, "template_name", TEMPLATE_DB_NAME);
        }
        if (getMeta(conn, "template_version") == null) {
            upsertMeta(conn, "template_version", TEMPLATE_DB_VERSION);
        }
    }

    // If needed, can use this to compare versions after "ensureVersionMeta(conn)" in init()
    private static String getMeta(Connection conn, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT meta_value FROM app_meta WHERE meta_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // Visible check to verify the installed DB version (the one in the hidden folder) using 'app_meta' in DB.
    private static void verifyInstalledDbVersion(Connection conn) throws SQLException {
        String schemaVersion = getMeta(conn, "schema_version");
        String templateName = getMeta(conn, "template_name");
        String templateVersion = getMeta(conn, "template_version");

        // For debugging purposes:
        System.out.printf(
                "Fetched  — schema_version: %s | template_name: %s | template_ver: %s%n" +
                        "Expected — schema_version: %s | template_name: %s | template_ver: %s%n",
                schemaVersion, templateName, templateVersion,
                APP_SCHEMA_VERSION, TEMPLATE_DB_NAME, TEMPLATE_DB_VERSION
        );

        if (!APP_SCHEMA_VERSION.equals(schemaVersion)) {
            System.err.println("Warning: local DB schema_version is '" + schemaVersion
                    + "', expected '" + APP_SCHEMA_VERSION + "'.");
        }

        if (!TEMPLATE_DB_NAME.equals(templateName)) {
            System.err.println("Warning: local DB template_name is '" + templateName
                    + "', expected '" + TEMPLATE_DB_NAME + "'.");
        }

        if (!TEMPLATE_DB_VERSION.equals(templateVersion)) {
            System.err.println("Warning: local DB template_version is '" + templateVersion
                    + "', expected '" + TEMPLATE_DB_VERSION + "'.");
        }
    }

    // If template DB is not already installed, this helper function will be used
    private static void installTemplateDbIfMissing() throws Exception {
        if (DbPaths.dbExists()) {
            return;
        }

        try (InputStream in = DbBootstrap.class.getResourceAsStream(TEMPLATE_DB_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing template DB resource: " + TEMPLATE_DB_RESOURCE);
            }

            Files.copy(in, DbPaths.dbFile(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Installed template DB to " + DbPaths.dbFile().toAbsolutePath());
        }
    }

    // ------------------------
    // CSV -> DB import (**REDUNDANT** - NOW USING TEMPLATE_DB WHICH HAS BEEN SEEDED ONCE WITH OLD CSV FILE)
    // ------------------------
    /*
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

     */


    // ------------------------
    // Carbon Intensity API cache (national)
    // ------------------------
    private static void cacheCurrentCarbonAndMix(Connection conn) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

//        // Current intensity
//        HttpRequest intensityReq = HttpRequest.newBuilder()
//                .uri(URI.create("https://api.carbonintensity.org.uk/intensity"))
//                .GET()
//                .build();
//
//        String intensityJson = client.send(intensityReq, HttpResponse.BodyHandlers.ofString()).body();
//        CarbonIntensityResponse intensity = gson.fromJson(intensityJson, CarbonIntensityResponse.class);
//
//        if (intensity != null && intensity.data != null && !intensity.data.isEmpty()) {
//            CarbonIntensityResponse.IntensityData d = intensity.data.get(0);
//
//            try (PreparedStatement ps = conn.prepareStatement("""
//                INSERT OR REPLACE INTO forecast_interval_cache(
//                  home_id, ts_start, ts_end, carbon_gco2_kwh, retrieved_at, source_json
//                ) VALUES(?,?,?,?,?,?)
//                """)) {
//                // Store carbon in forecast cache too (so Forecasts page has something immediately)
//                ps.setString(1, HOME_ID);
//                ps.setString(2, toAppTs(d.from));
//                ps.setString(3, toAppTs(d.to));
//                ps.setDouble(4, d.intensity != null && d.intensity.actual != null ? d.intensity.actual : d.intensity.forecast);
//                ps.setString(5, LocalDateTime.now().format(TS_FMT));
//                ps.setString(6, "{\"source\":\"carbonintensity\",\"endpoint\":\"/intensity\"}");
//                ps.executeUpdate();
//            }
//        }

        // ===========================
        // CARBON: store next-24h forecast into forecast_interval_cache
        // Fixes 400 by using a 'from' timestamp provided by the API itself (guaranteed valid).
        // ====================
        try {
            // 1) Get a valid 'from' value from the API (current half-hour window)
            HttpRequest curReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.carbonintensity.org.uk/intensity"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> curResp = client.send(curReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("Carbon current status=" + curResp.statusCode());
            if (curResp.statusCode() != 200) {
                System.err.println("Carbon current failed body=" + curResp.body());
                // Don't throw: still let generation mix run below
                return;
            }

            CarbonIntensityResponse cur = gson.fromJson(curResp.body(), CarbonIntensityResponse.class);
            if (cur == null || cur.data == null || cur.data.isEmpty() || cur.data.get(0).from == null) {
                System.err.println("Carbon current parse failed (missing data[0].from)");
                return;
            }

            // This value is already in the exact format the API expects, e.g. 2026-03-04T20:00Z
            String from = cur.data.get(0).from;

            // 2) Fetch 24h forward forecast using that exact from
            String fwUrl = "https://api.carbonintensity.org.uk/intensity/" + from + "/fw24h";
            System.out.println("Carbon fw24h url=" + fwUrl);

            HttpRequest fwReq = HttpRequest.newBuilder()
                    .uri(URI.create(fwUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> fwResp = client.send(fwReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("Carbon fw24h status=" + fwResp.statusCode());
            if (fwResp.statusCode() != 200) {
                System.err.println("Carbon fw24h failed body=" + fwResp.body());
                return;
            }

            CarbonIntensityResponse fw = gson.fromJson(fwResp.body(), CarbonIntensityResponse.class);
            if (fw == null || fw.data == null || fw.data.isEmpty()) return;

            // 3) Persist every half-hour window into forecast_interval_cache
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("""
        INSERT OR REPLACE INTO forecast_interval_cache(
          home_id, ts_start, ts_end, carbon_gco2_kwh, retrieved_at, source_json
        ) VALUES(?,?,?,?,?,?)
        """)) {

                String retrievedAt = LocalDateTime.now().format(TS_FMT);

                for (CarbonIntensityResponse.IntensityData d : fw.data) {
                    Integer v = (d.intensity != null && d.intensity.actual != null)
                            ? d.intensity.actual
                            : (d.intensity != null ? d.intensity.forecast : null);
                    if (v == null) continue;

                    ps.setString(1, HOME_ID);
                    ps.setString(2, toAppTs(d.from));
                    ps.setString(3, toAppTs(d.to));
                    ps.setDouble(4, v);
                    ps.setString(5, retrievedAt);
                    ps.setString(6, "{\"source\":\"carbonintensity\",\"endpoint\":\"/intensity/{from}/fw24h\"}");
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            System.err.println("Carbon cache failed:");
            e.printStackTrace();
        }

        // Current generation mix - with normalised rows
        HttpRequest genReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.carbonintensity.org.uk/generation"))
                .GET()
                .build();

//        String genJson = client.send(genReq, HttpResponse.BodyHandlers.ofString()).body();
        // Status and failure handling included
        // GENERATION MIX: fetch + store normalized rows
        HttpResponse<String> genResp = client.send(genReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Generation status=" + genResp.statusCode());
        if (genResp.statusCode() != 200) {
            throw new RuntimeException("Generation API failed: " + genResp.statusCode() + " body=" + genResp.body());
        }
        String genJson = genResp.body();

        // 1) Insert the current window (data object)
        upsertGenerationMixRows(conn, genJson, "national", null);

        // 2) Backfill last 24h (so the  “last 4 hours” UI works immediately)
        // The API docs list /generation/{from}/pt24h. :contentReference[oaicite:1]{index=1}
        String fromForPt24h = extractGenerationToTimestamp(genJson);
        if (fromForPt24h != null) {
            // IMPORTANT: do NOT URL-encode this timestamp when putting it in the path
            String ptUrl = "https://api.carbonintensity.org.uk/generation/" + fromForPt24h + "/pt24h";

            HttpRequest ptReq = HttpRequest.newBuilder()
                    .uri(URI.create(ptUrl))
                    .GET()
                    .build();

            HttpResponse<String> ptResp = client.send(ptReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("Generation pt24h url=" + ptUrl);
            System.out.println("Generation pt24h status=" + ptResp.statusCode());
            if (ptResp.statusCode() != 200) {
                throw new RuntimeException("Generation pt24h API failed: " + ptResp.statusCode() + " body=" + ptResp.body());
            }

            String ptJson = ptResp.body();
            upsertGenerationMixRows(conn, ptJson, "national", null);
        }

        // 3) Cleanup (toavoid DB growing forever)
        cleanupOldGenerationMix(conn, "national", null, 72);
    }

    private static void cacheOctopusTariffs(Connection conn) throws Exception {
        // --------------
        // OCTOPUS: choose from/to here
        // from = now (UTC) rounded down to 30-min boundary
        // to   = from + 48h (you can change to 24h if you want)
        // --------------------------

        OffsetDateTime from = OctopusEnergyTariffService.roundDownToHalfHourUtc(
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        OffsetDateTime to = from.plusHours(48);

        OctopusEnergyTariffService octopus = new OctopusEnergyTariffService();
        var rates = octopus.getUnitRates(from, to);

        System.out.println("Octopus rates fetched=" + rates.size() + " from=" + from + " to=" + to);

        ForecastIntervalCacheWriter.upsertTariffRates(conn, HOME_ID, rates);

        upsertMeta(conn, "last_octopus_cache_at", LocalDateTime.now().format(TS_FMT));
    }

    private static void cacheOpenMeteoSolar(Connection conn) throws Exception {
        // Get lat/lon from home_profile (seeded values in seed.sql)
        double lat;
        double lon;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT lat, lon FROM home_profile WHERE home_id=?")) {
            ps.setString(1, HOME_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Missing home_profile for " + HOME_ID);
                }
                lat = rs.getDouble("lat");
                lon = rs.getDouble("lon");
            }
        }

        OpenMeteoSolarService solar = new OpenMeteoSolarService();
        var points = solar.fetchHourlyShortwave(lat, lon, 2); // next 2 days hourly
        System.out.println("Open-Meteo solar points fetched=" + points.size());

        ForecastIntervalCacheWriter.upsertSolarRadiation(conn, HOME_ID, points);
        upsertMeta(conn, "last_solar_cache_at", LocalDateTime.now().format(TS_FMT));
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

    // Helper methods for generation mix - for e.g. "(national, null, 2026-02-27 00:00:00, fuel=wind, perc=58.1, …)"
    private static void upsertGenerationMixRows(Connection conn, String json, String scope, Integer regionId) throws SQLException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement dataEl = root.get("data");
        if (dataEl == null) return;

        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement("""
        INSERT OR REPLACE INTO grid_generation_mix_interval(
          scope, region_id, ts_start, ts_end, fuel, perc, retrieved_at
        ) VALUES(?,?,?,?,?,?,?)
        """)) {

            if (dataEl.isJsonObject()) {
                insertOneGenWindow(ps, dataEl.getAsJsonObject(), scope, regionId);
            } else if (dataEl.isJsonArray()) {
                JsonArray arr = dataEl.getAsJsonArray();
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) insertOneGenWindow(ps, e.getAsJsonObject(), scope, regionId);
                }
            }

            ps.executeBatch();
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void insertOneGenWindow(PreparedStatement ps, JsonObject window, String scope, Integer regionId) throws SQLException {
        String fromIso = getAsString(window, "from");
        String toIso = getAsString(window, "to");
        JsonArray mix = window.getAsJsonArray("generationmix");
        if (mix == null) return;

        String tsStart = toAppTs(fromIso);
        String tsEnd = toAppTs(toIso);
        String retrievedAt = LocalDateTime.now().format(TS_FMT);

        for (JsonElement fuelEl : mix) {
            if (!fuelEl.isJsonObject()) continue;
            JsonObject f = fuelEl.getAsJsonObject();
            String fuel = getAsString(f, "fuel");
            Double perc = f.has("perc") && !f.get("perc").isJsonNull() ? f.get("perc").getAsDouble() : null;
            if (fuel == null || perc == null) continue;

            ps.setString(1, scope);
            if (regionId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, regionId);
            ps.setString(3, tsStart);
            ps.setString(4, tsEnd);
            ps.setString(5, fuel);
            ps.setDouble(6, perc);
            ps.setString(7, retrievedAt);
            ps.addBatch();
        }
    }

    private static String extractGenerationToTimestamp(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return null;
            // Use "to" so pt24h backfill goes up to the most recent known window
            return getAsString(data, "to");
        } catch (Exception e) {
            return null;
        }
    }

    private static String getAsString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private static void cleanupOldGenerationMix(Connection conn, String scope, Integer regionId, int keepHours) throws SQLException {
        // SQLite datetime('now', '-72 hours') returns "YYYY-MM-DD HH:MM:SS"
        // which matches our stored "yyyy-MM-dd HH:mm:ss" format.
        String sql;
        if (regionId == null) {
            sql = "DELETE FROM grid_generation_mix_interval " +
                    "WHERE scope=? AND region_id IS NULL AND ts_start < datetime('now', ?)";
        } else {
            sql = "DELETE FROM grid_generation_mix_interval " +
                    "WHERE scope=? AND region_id=? AND ts_start < datetime('now', ?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scope);
            int idx = 2;
            if (regionId != null) ps.setInt(idx++, regionId);
            ps.setString(idx, "-" + keepHours + " hours");
            ps.executeUpdate();
        }
    }

    public static void refreshCarbonAndMix(Connection conn) throws Exception {
        cacheCurrentCarbonAndMix(conn);
    }

    public static void refreshOctopus(Connection conn) throws Exception {
        cacheOctopusTariffs(conn);
    }

    public static void refreshSolar(Connection conn) throws Exception {
        cacheOpenMeteoSolar(conn);
    }

}
