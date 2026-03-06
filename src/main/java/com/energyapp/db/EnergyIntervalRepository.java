package com.energyapp.db;

import com.energyapp.model.EnergyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnergyIntervalRepository {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // These match the IDs you seeded in seed.sql in Part 1
    private static final String DEV_EV   = "DEV_EV_1";
    private static final String DEV_WM   = "DEV_WM_1";
    private static final String DEV_HEAT = "DEV_HEAT_1";
    private static final String DEV_BASE = "DEV_BASE_1";

    /** Loads ALL energy intervals for playback, ordered by time. */
    public List<EnergyRecord> fetchAll(String homeId) {
        try (Connection conn = Sqlite.openConnection()) {
            Map<String, DeviceUsage> usageByTs = loadDeviceUsage(conn, homeId);

            String sql = """
                SELECT ts_start, time_label, price_p_per_kwh, carbon_gco2_kwh,
                       total_load_kwh, solar_gen_kwh, battery_level_pct, grid_import_kwh
                FROM energy_interval
                WHERE home_id = ?
                ORDER BY ts_start
                """;

            List<EnergyRecord> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, homeId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tsStr = rs.getString("ts_start");
                        LocalDateTime ts = LocalDateTime.parse(tsStr, TS_FMT);

                        String timeLabel = rs.getString("time_label");
                        double price = getDoubleOr0(rs, "price_p_per_kwh");
                        double carbon = getDoubleOr0(rs, "carbon_gco2_kwh");
                        double totalLoad = getDoubleOr0(rs, "total_load_kwh");
                        double solarGen = getDoubleOr0(rs, "solar_gen_kwh");
                        int batteryLevel = getIntOr0(rs, "battery_level_pct");
                        double gridImport = getDoubleOr0(rs, "grid_import_kwh");

                        DeviceUsage du = usageByTs.getOrDefault(tsStr, new DeviceUsage());
                        out.add(new EnergyRecord(
                                ts, timeLabel, gridImport, price, carbon, totalLoad,
                                solarGen, batteryLevel,
                                du.evKwh, du.wmKwh, du.heatKwh, du.baseKwh
                        ));
                    }
                }
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch energy intervals from DB", e);
        }
    }

    private Map<String, DeviceUsage> loadDeviceUsage(Connection conn, String homeId) throws SQLException {
        String sql = """
            SELECT ts_start, device_id, kwh
            FROM device_usage_interval
            WHERE home_id = ?
            """;

        Map<String, DeviceUsage> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ts = rs.getString("ts_start");
                    String dev = rs.getString("device_id");
                    double kwh = rs.getDouble("kwh");

                    DeviceUsage du = map.computeIfAbsent(ts, k -> new DeviceUsage());
                    if (DEV_EV.equals(dev)) du.evKwh = kwh;
                    else if (DEV_WM.equals(dev)) du.wmKwh = kwh;
                    else if (DEV_HEAT.equals(dev)) du.heatKwh = kwh;
                    else if (DEV_BASE.equals(dev)) du.baseKwh = kwh;
                }
            }
        }
        return map;
    }

    private static double getDoubleOr0(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? 0.0 : v;
    }

    private static int getIntOr0(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? 0 : v;
    }

    private static final class DeviceUsage {
        double evKwh;
        double wmKwh;
        double heatKwh;
        double baseKwh;
    }
}