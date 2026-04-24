package com.energyapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DeviceRepository {

    private static final DateTimeFormatter DB_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public DeviceRepository() {
        DbPaths.ensureDataDirExists();
    }

    public List<CanonicalDeviceRecord> fetchActiveByHome(String homeId) {
        String sql = """
            SELECT device_id, home_id, display_name, device_type, category, icon_key,
                   typical_power_w, typical_cycle_kwh, typical_duration_mins,
                   power_state, status, current_draw_w,
                   green_energy_only, ready_by_time, schedule_time, price_cutoff_p_per_kwh, policy,
                   next_run_window_start, next_run_window_end,
                   next_run_est_cost_gbp, next_run_est_co2_kg,
                   next_run_source_solar_pct, next_run_source_grid_pct,
                   active_run_until, run_mode, last_run_started_at, last_run_ended_at,
                   last_scheduled_run_date, skip_next_run, location_label,
                   created_at, updated_at, deleted_at
            FROM device
            WHERE home_id = ? AND deleted_at IS NULL
            ORDER BY display_name, device_id
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);

            List<CanonicalDeviceRecord> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch canonical devices", e);
        }
    }

    public Optional<CanonicalDeviceRecord> findById(String deviceId) {
        String sql = """
            SELECT device_id, home_id, display_name, device_type, category, icon_key,
                   typical_power_w, typical_cycle_kwh, typical_duration_mins,
                   power_state, status, current_draw_w,
                   green_energy_only, ready_by_time, schedule_time, price_cutoff_p_per_kwh, policy,
                   next_run_window_start, next_run_window_end,
                   next_run_est_cost_gbp, next_run_est_co2_kg,
                   next_run_source_solar_pct, next_run_source_grid_pct,
                   active_run_until, run_mode, last_run_started_at, last_run_ended_at,
                   last_scheduled_run_date, skip_next_run, location_label,
                   created_at, updated_at, deleted_at
            FROM device
            WHERE device_id = ?
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find canonical device " + deviceId, e);
        }
    }

    public void insert(CanonicalDeviceRecord record) {
        String sql = """
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
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindRecord(ps, record, false);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert canonical device " + record.deviceId(), e);
        }
    }

    public void update(CanonicalDeviceRecord record) {
        String sql = """
            UPDATE device
            SET home_id = ?,
                display_name = ?,
                device_type = ?,
                category = ?,
                icon_key = ?,
                typical_power_w = ?,
                typical_cycle_kwh = ?,
                typical_duration_mins = ?,
                power_state = ?,
                status = ?,
                current_draw_w = ?,
                green_energy_only = ?,
                ready_by_time = ?,
                schedule_time = ?,
                price_cutoff_p_per_kwh = ?,
                policy = ?,
                next_run_window_start = ?,
                next_run_window_end = ?,
                next_run_est_cost_gbp = ?,
                next_run_est_co2_kg = ?,
                next_run_source_solar_pct = ?,
                next_run_source_grid_pct = ?,
                active_run_until = ?,
                run_mode = ?,
                last_run_started_at = ?,
                last_run_ended_at = ?,
                last_scheduled_run_date = ?,
                skip_next_run = ?,
                location_label = ?,
                created_at = ?,
                updated_at = ?,
                deleted_at = ?
            WHERE device_id = ?
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindRecord(ps, record, true);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update canonical device " + record.deviceId(), e);
        }
    }

    public void softDelete(String deviceId, Instant deletedAt) {
        String sql = """
            UPDATE device
            SET deleted_at = ?, updated_at = ?
            WHERE device_id = ?
            """;

        String timestamp = DB_TS_FMT.format(deletedAt);
        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, timestamp);
            ps.setString(2, timestamp);
            ps.setString(3, deviceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to soft delete canonical device " + deviceId, e);
        }
    }

    private static CanonicalDeviceRecord mapRow(ResultSet rs) throws SQLException {
        return new CanonicalDeviceRecord(
                rs.getString("device_id"),
                rs.getString("home_id"),
                rs.getString("display_name"),
                rs.getString("device_type"),
                rs.getString("category"),
                rs.getString("icon_key"),
                getNullableDouble(rs, "typical_power_w"),
                getNullableDouble(rs, "typical_cycle_kwh"),
                getNullableInteger(rs, "typical_duration_mins"),
                rs.getString("power_state"),
                rs.getString("status"),
                getNullableDouble(rs, "current_draw_w"),
                rs.getInt("green_energy_only") == 1,
                rs.getString("ready_by_time"),
                rs.getString("schedule_time"),
                getNullableDouble(rs, "price_cutoff_p_per_kwh"),
                rs.getString("policy"),
                rs.getString("next_run_window_start"),
                rs.getString("next_run_window_end"),
                getNullableDouble(rs, "next_run_est_cost_gbp"),
                getNullableDouble(rs, "next_run_est_co2_kg"),
                getNullableDouble(rs, "next_run_source_solar_pct"),
                getNullableDouble(rs, "next_run_source_grid_pct"),
                rs.getString("active_run_until"),
                rs.getString("run_mode"),
                rs.getString("last_run_started_at"),
                rs.getString("last_run_ended_at"),
                rs.getString("last_scheduled_run_date"),
                rs.getInt("skip_next_run") == 1,
                rs.getString("location_label"),
                rs.getString("created_at"),
                rs.getString("updated_at"),
                rs.getString("deleted_at")
        );
    }

    private static void bindRecord(PreparedStatement ps, CanonicalDeviceRecord record, boolean includeWhereId)
            throws SQLException {
        int idx = 1;
        if (!includeWhereId) {
            ps.setString(idx++, record.deviceId());
        }
        ps.setString(idx++, record.homeId());
        ps.setString(idx++, record.displayName());
        setNullableString(ps, idx++, record.deviceType());
        ps.setString(idx++, record.category());
        setNullableString(ps, idx++, record.iconKey());
        setNullableDouble(ps, idx++, record.typicalPowerW());
        setNullableDouble(ps, idx++, record.typicalCycleKwh());
        setNullableInteger(ps, idx++, record.typicalDurationMins());
        setNullableString(ps, idx++, record.powerState());
        setNullableString(ps, idx++, record.status());
        setNullableDouble(ps, idx++, record.currentDrawW());
        ps.setInt(idx++, record.greenEnergyOnly() ? 1 : 0);
        setNullableString(ps, idx++, record.readyByTime());
        setNullableString(ps, idx++, record.scheduleTime());
        setNullableDouble(ps, idx++, record.priceCutoffPPerKwh());
        setNullableString(ps, idx++, record.policy());
        setNullableString(ps, idx++, record.nextRunWindowStart());
        setNullableString(ps, idx++, record.nextRunWindowEnd());
        setNullableDouble(ps, idx++, record.nextRunEstCostGbp());
        setNullableDouble(ps, idx++, record.nextRunEstCo2Kg());
        setNullableDouble(ps, idx++, record.nextRunSourceSolarPct());
        setNullableDouble(ps, idx++, record.nextRunSourceGridPct());
        setNullableString(ps, idx++, record.activeRunUntil());
        setNullableString(ps, idx++, record.runMode());
        setNullableString(ps, idx++, record.lastRunStartedAt());
        setNullableString(ps, idx++, record.lastRunEndedAt());
        setNullableString(ps, idx++, record.lastScheduledRunDate());
        ps.setInt(idx++, record.skipNextRun() ? 1 : 0);
        setNullableString(ps, idx++, record.locationLabel());
        ps.setString(idx++, record.createdAt());
        ps.setString(idx++, record.updatedAt());
        setNullableString(ps, idx++, record.deletedAt());
        if (includeWhereId) {
            ps.setString(idx, record.deviceId());
        }
    }

    private static Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private static Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.REAL);
        } else {
            ps.setDouble(index, value);
        }
    }

    private static void setNullableInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
