package com.energyapp.db;

import com.energyapp.model.Geocode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public final class HomeProfileRepository {

    public record HomeProfileSnapshot(
            String firstName,
            String cityName,
            Geocode location
    ) {
        public boolean hasFirstName() {
            return !isBlank(firstName);
        }

        public boolean hasCity() {
            return !isBlank(cityName);
        }

        public boolean isSetupComplete() {
            return hasFirstName() && hasCity() && location != null;
        }
    }

    public void saveProfile(String homeId, String firstName, String cityName, Geocode code) {
        String sql = "UPDATE home_profile SET first_name = ?, city_name = ?, lat = ?, lon = ? WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, normalize(firstName));
            pstmt.setString(2, normalize(cityName));
            pstmt.setDouble(3, code.latitude());
            pstmt.setDouble(4, code.longitude());
            pstmt.setString(5, homeId);

            ensureSingleRowUpdated(homeId, pstmt.executeUpdate(), "profile");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist home profile for " + homeId, e);
        }
    }

    public void updateHomeCity(String homeId, String cityName, Geocode code) {
        String sql = "UPDATE home_profile SET city_name = ?, lat = ?, lon = ? WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, normalize(cityName));
            pstmt.setDouble(2, code.latitude());
            pstmt.setDouble(3, code.longitude());
            pstmt.setString(4, homeId);

            ensureSingleRowUpdated(homeId, pstmt.executeUpdate(), "city");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist home city for " + homeId, e);
        }
    }

    public void updateHomeLocation(String homeId, Geocode code) {
        String sql = "UPDATE home_profile SET lat = ?, lon = ? WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, code.latitude());
            pstmt.setDouble(2, code.longitude());
            pstmt.setString(3, homeId);

            ensureSingleRowUpdated(homeId, pstmt.executeUpdate(), "location");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist home location for " + homeId, e);
        }
    }

    public Geocode getHomeLocation(String homeId) {
        HomeProfileSnapshot profile = getProfile(homeId);
        return profile == null ? null : profile.location();
    }

    public String getFirstName(String homeId) {
        HomeProfileSnapshot profile = getProfile(homeId);
        return profile == null ? null : profile.firstName();
    }

    public String getCityName(String homeId) {
        HomeProfileSnapshot profile = getProfile(homeId);
        return profile == null ? null : profile.cityName();
    }

    public HomeProfileSnapshot getProfile(String homeId) {
        String sql = "SELECT first_name, city_name, lat, lon FROM home_profile WHERE home_id = ?";

        try (Connection conn = Sqlite.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, homeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Geocode location = hasLocation(rs)
                            ? new Geocode(rs.getDouble("lat"), rs.getDouble("lon"))
                            : null;
                    return new HomeProfileSnapshot(
                            rs.getString("first_name"),
                            rs.getString("city_name"),
                            location
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load home profile for " + homeId, e);
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

            ensureSingleRowUpdated(homeId, pstmt.executeUpdate(), "weekly budget");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist weekly budget for home " + homeId, e);
        }
    }

    private static boolean hasLocation(ResultSet rs) throws SQLException {
        return rs.getObject("lat") != null && rs.getObject("lon") != null;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void ensureSingleRowUpdated(String homeId, int updatedRows, String label) {
        if (updatedRows != 1) {
            throw new IllegalStateException("Expected to update exactly one home_profile " + label
                    + " row for " + homeId + ", but updated " + updatedRows);
        }
    }
}
