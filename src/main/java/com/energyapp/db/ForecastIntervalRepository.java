package com.energyapp.db;

import com.energyapp.model.EnergyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class ForecastIntervalRepository {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String HOME_ID = "HOME_1";

    public List<EnergyRecord> fetchTodayTariffForecast() {
        return fetchTariffForecast(HOME_ID, LocalDate.now(ZoneOffset.UTC));
    }

    public List<EnergyRecord> fetchTariffForecast(String homeId, LocalDate day) {
        String sql = """
            SELECT ts_start, tariff_p_per_kwh
            FROM forecast_interval_cache
            WHERE home_id = ?
              AND tariff_p_per_kwh IS NOT NULL
              AND ts_start >= ?
              AND ts_start < ?
            ORDER BY ts_start
            """;

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime nextDayStart = day.plusDays(1).atStartOfDay();

        List<EnergyRecord> out = new ArrayList<>();
        try (Connection conn = Sqlite.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, homeId);
            ps.setString(2, dayStart.format(TS_FMT));
            ps.setString(3, nextDayStart.format(TS_FMT));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tsStr = rs.getString("ts_start");
                    LocalDateTime timestamp = LocalDateTime.parse(tsStr, TS_FMT);
                    double price = rs.getDouble("tariff_p_per_kwh");
                    out.add(new EnergyRecord(
                            timestamp,
                            timestamp.toLocalTime().format(TIME_FMT),
                            0.0,
                            price,
                            0.0,
                            0.0,
                            0.0,
                            0,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                    ));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch forecast tariff intervals from DB", e);
        }
    }
}