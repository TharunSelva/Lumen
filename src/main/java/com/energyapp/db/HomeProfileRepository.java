package com.energyapp.db;

import com.energyapp.model.Geocode;

import java.sql.Connection;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class HomeProfileRepository {

    public void updateHomeLocation(String homeId, Geocode code) {
        String sql = "UPDATE home_profile SET lat = ?, lon = ? WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, code.latitude());
            pstmt.setDouble(2, code.longitude());
            pstmt.setString(3, homeId);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Geocode getHomeLocation(String homeId) {
        String sql = "SELECT lat, lon FROM home_profile WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, homeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getObject("lat") != null && rs.getObject("lon") != null) {
                        double lat = rs.getDouble("lat");
                        double lon = rs.getDouble("lon");
                        return new Geocode(lat, lon);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Double getWeeklyBudgetGbp(String homeId) {
        String sql = "SELECT weekly_budget_gbp FROM home_profile WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, homeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("weekly_budget_gbp") == null ? null : rs.getDouble("weekly_budget_gbp");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load weekly budget for home " + homeId, e);
        }
        return null;
    }

    public void setWeeklyBudgetGbp(String homeId, Double amount) {
        String sql = "UPDATE home_profile SET weekly_budget_gbp = ? WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (amount == null) {
                pstmt.setNull(1, Types.REAL);
            } else {
                pstmt.setDouble(1, amount);
            }
            pstmt.setString(2, homeId);

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows != 1) {
                throw new IllegalStateException("Expected to update exactly one home_profile row for " + homeId
                        + ", but updated " + updatedRows);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist weekly budget for home " + homeId, e);
        }
    }

}
