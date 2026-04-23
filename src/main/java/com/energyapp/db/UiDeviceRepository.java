package com.energyapp.db;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class UiDeviceRepository {

    private static final DateTimeFormatter SCHEDULE_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public UiDeviceRepository() {
        DbPaths.ensureDataDirExists();
        ensureTableExists();
    }

    public List<Device> fetchAll(String homeId) {
        String sql = """
            SELECT id, device_name, device_type, price_cutoff, schedule_time, is_on
            FROM ui_device
            WHERE home_id = ?
            ORDER BY id
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);

            List<Device> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Device device = mapRowToDevice(rs);
                    if (device != null) {
                        out.add(device);
                    }
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch persisted UI devices", e);
        }
    }

    public void insert(String homeId, Device device) {
        String sql = """
            INSERT INTO ui_device(
              home_id, device_name, device_type, price_cutoff, schedule_time, is_on, created_at, updated_at
            ) VALUES(?,?,?,?,?,?,datetime('now'),datetime('now'))
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindDevice(ps, homeId, device);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    device.setPersistedId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert UI device", e);
        }
    }

    public void upsert(String homeId, Device device) {
        if (device.getPersistedId() == null) {
            insert(homeId, device);
            return;
        }

        String sql = """
            UPDATE ui_device
            SET device_name = ?,
                device_type = ?,
                price_cutoff = ?,
                schedule_time = ?,
                is_on = ?,
                updated_at = datetime('now')
            WHERE id = ? AND home_id = ?
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, device.getDeviceName());
            ps.setString(2, device.getDeviceType().name());
            setNullableDouble(ps, 3, getPriceCutoffOrNull(device));
            setNullableString(ps, 4, getScheduleStringOrNull(device));
            ps.setInt(5, device.isOn() ? 1 : 0);
            ps.setLong(6, device.getPersistedId());
            ps.setString(7, homeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update UI device", e);
        }
    }

    private static Device mapRowToDevice(ResultSet rs) throws SQLException {
        String name = rs.getString("device_name");
        String typeText = rs.getString("device_type");

        DeviceType type;
        try {
            type = DeviceType.valueOf(typeText);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        Device device;
        if (type == DeviceType.SMART_PLUG) {
            SmartPlug smartPlug = new SmartPlug(name, type);
            double cutoff = rs.getDouble("price_cutoff");
            if (!rs.wasNull()) {
                smartPlug.setPriceCutoff(cutoff);
            }
            device = smartPlug;
        } else {
            ScheduledDevice scheduled = new ScheduledDevice(name, type);
            String scheduleText = rs.getString("schedule_time");
            if (scheduleText != null && !scheduleText.isBlank()) {
                try {
                    scheduled.setSchedule(LocalTime.parse(scheduleText, SCHEDULE_FMT));
                } catch (DateTimeParseException ignored) {
                    scheduled.setSchedule(null);
                }
            }
            device = scheduled;
        }

        device.setPersistedId(rs.getLong("id"));
        device.setOn(rs.getInt("is_on") == 1);
        return device;
    }

    private static void bindDevice(PreparedStatement ps, String homeId, Device device) throws SQLException {
        ps.setString(1, homeId);
        ps.setString(2, device.getDeviceName());
        ps.setString(3, device.getDeviceType().name());
        setNullableDouble(ps, 4, getPriceCutoffOrNull(device));
        setNullableString(ps, 5, getScheduleStringOrNull(device));
        ps.setInt(6, device.isOn() ? 1 : 0);
    }

    private static Double getPriceCutoffOrNull(Device device) {
        if (device instanceof SmartPlug) {
            return ((SmartPlug) device).getPriceCutoff();
        }
        return null;
    }

    private static String getScheduleStringOrNull(Device device) {
        if (device instanceof ScheduledDevice) {
            LocalTime schedule = ((ScheduledDevice) device).getSchedule();
            return schedule == null ? null : schedule.format(SCHEDULE_FMT);
        }
        return null;
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.REAL);
        } else {
            ps.setDouble(idx, value);
        }
    }

    private static void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, value);
        }
    }

    private void ensureTableExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS ui_device (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                home_id       TEXT NOT NULL,
                device_name   TEXT NOT NULL,
                device_type   TEXT NOT NULL,
                price_cutoff  REAL,
                schedule_time TEXT,
                is_on         INTEGER NOT NULL DEFAULT 1,
                created_at    TEXT NOT NULL,
                updated_at    TEXT NOT NULL
            )
            """;

        try (Connection conn = Sqlite.openConnection();
             Statement s = conn.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure ui_device table exists", e);
        }
    }
}
