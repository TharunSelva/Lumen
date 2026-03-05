package com.energyapp.db;

import com.energyapp.service.OctopusEnergyTariffService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ForecastIntervalCacheWriter {

    private static final DateTimeFormatter DB_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ForecastIntervalCacheWriter() {}

    /**
     * Upserts Octopus tariff rates into forecast_interval_cache.tariff_p_per_kwh.
     * Does NOT overwrite carbon/solar columns.
     */
    public static void upsertTariffRates(
            Connection conn,
            String homeId,
            List<OctopusEnergyTariffService.UnitRate> rates
    ) throws SQLException {

        String sql = """
            INSERT INTO forecast_interval_cache(
              home_id, ts_start, ts_end, tariff_p_per_kwh, retrieved_at, source_json
            ) VALUES(?,?,?,?,?,?)
            ON CONFLICT(home_id, ts_start) DO UPDATE SET
              tariff_p_per_kwh = excluded.tariff_p_per_kwh,
              ts_end = COALESCE(excluded.ts_end, forecast_interval_cache.ts_end),
              retrieved_at = excluded.retrieved_at
            """;

        String retrievedAt = LocalDateTime.now().format(DB_TS);

        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OctopusEnergyTariffService.UnitRate r : rates) {
                ps.setString(1, homeId);
                ps.setString(2, toDbTs(r.validFromUtc));
                ps.setString(3, toDbTs(r.validToUtc));
                ps.setDouble(4, r.pencePerKwhIncVat);
                ps.setString(5, retrievedAt);
                ps.setString(6, "{\"source\":\"octopus\",\"endpoint\":\"standard-unit-rates\"}");
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static String toDbTs(OffsetDateTime t) {
        return t.withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
                .format(DB_TS);
    }
}