package com.energyapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class HomeEnergyAssetsRepository {

    public HomeEnergyAssets fetchByHomeId(String homeId) {
        try (Connection conn = Sqlite.openConnection()) {
            boolean hasSolarCapacityColumn = hasColumn(conn, "home_energy_assets", "solar_capacity_kw");
            String sql = hasSolarCapacityColumn
                    ? """
                    SELECT has_solar, has_battery, battery_capacity_kwh, min_soc_pct, max_discharge_kw, solar_capacity_kw
                    FROM home_energy_assets
                    WHERE home_id = ?
                    """
                    : """
                    SELECT has_solar, has_battery, battery_capacity_kwh, min_soc_pct, max_discharge_kw
                    FROM home_energy_assets
                    WHERE home_id = ?
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, homeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return HomeEnergyAssets.defaults();
                    }

                    Double solarCapacity = hasSolarCapacityColumn
                            ? getNullableDouble(rs, "solar_capacity_kw")
                            : null;

                    return new HomeEnergyAssets(
                            getBooleanFlag(rs, "has_solar"),
                            getBooleanFlag(rs, "has_battery"),
                            getNullableDouble(rs, "battery_capacity_kwh"),
                            getNullableInteger(rs, "min_soc_pct"),
                            getNullableDouble(rs, "max_discharge_kw"),
                            solarCapacity
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch home energy assets for " + homeId, e);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
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

    private boolean getBooleanFlag(ResultSet rs, String column) throws SQLException {
        return rs.getInt(column) == 1;
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    public record HomeEnergyAssets(
            boolean hasSolar,
            boolean hasBattery,
            Double batteryCapacityKwh,
            Integer minSocPct,
            Double maxDischargeKw,
            Double solarCapacityKw
    ) {
        public static HomeEnergyAssets defaults() {
            return new HomeEnergyAssets(false, false, null, null, null, null);
        }

        public double solarCapacityOrDefault(double fallbackKw) {
            return solarCapacityKw != null && solarCapacityKw > 0.0 ? solarCapacityKw : fallbackKw;
        }
    }
}
