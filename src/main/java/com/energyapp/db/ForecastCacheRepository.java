package com.energyapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class ForecastCacheRepository {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<ForecastWindowRecord> fetchForecastWindow(
            String homeId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive,
            ZoneId zoneId
    ) {
        String sql = """
            SELECT ts_start, ts_end, tariff_p_per_kwh, carbon_gco2_kwh,
                   solar_shortwave_wm2, battery_soc_pct_pred, battery_action
            FROM forecast_interval_cache
            WHERE home_id = ?
              AND ts_start >= ?
              AND ts_start < ?
            ORDER BY ts_start
            """;

        ZonedDateTime utcStart = fromInclusive.atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime utcEnd = toExclusive.atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC);

        List<ForecastWindowRecord> windows = new ArrayList<>();
        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);
            ps.setString(2, utcStart.toLocalDateTime().format(TS_FMT));
            ps.setString(3, utcEnd.toLocalDateTime().format(TS_FMT));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    windows.add(new ForecastWindowRecord(
                            toLocalTime(rs.getString("ts_start"), zoneId),
                            toLocalTime(rs.getString("ts_end"), zoneId),
                            getNullableDouble(rs, "tariff_p_per_kwh"),
                            getNullableDouble(rs, "carbon_gco2_kwh"),
                            getNullableDouble(rs, "solar_shortwave_wm2"),
                            getNullableDouble(rs, "battery_soc_pct_pred"),
                            rs.getString("battery_action")
                    ));
                }
            }
            return windows;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch forecast cache window", e);
        }
    }

    public LocalDateTime findFirstWindowStart(String homeId, ZoneId zoneId) {
        String sql = """
            SELECT ts_start
            FROM forecast_interval_cache
            WHERE home_id = ?
            ORDER BY ts_start
            LIMIT 1
            """;

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toLocalTime(rs.getString("ts_start"), zoneId);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve first forecast cache window", e);
        }
    }

    private static LocalDateTime toLocalTime(String rawTimestamp, ZoneId zoneId) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return null;
        }

        String normalized = rawTimestamp.trim();

        try {
            return LocalDateTime.parse(normalized, TS_FMT)
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(zoneId)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Fall through to ISO-8601 parsing used by the runtime forecast cache.
        }

        try {
            return OffsetDateTime.parse(normalized)
                    .atZoneSameInstant(zoneId)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Fall through to instant parsing for raw UTC instants.
        }

        return Instant.parse(normalized)
                .atZone(zoneId)
                .toLocalDateTime();
    }

    private static Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}
