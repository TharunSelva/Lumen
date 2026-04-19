package com.energyapp.db;

import com.energyapp.model.Geocode;

import java.sql.Connection;
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

}
