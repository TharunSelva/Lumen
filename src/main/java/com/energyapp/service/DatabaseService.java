package com.energyapp.service;

import java.sql.*;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;


public class DatabaseService {
    private static Connection conn = null;
    SimulationService simulationService;

    public DatabaseService() {
        try {
            createEnergyRecordsTable();
        }catch (Exception e) {
           e.printStackTrace();
           System.out.println("Failed to create energy records table");
        }
        simulationService = new SimulationService();
        simulationService.startSimulation();
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                addData(newVal);
            }
        });
    }

    /**
     * Provides a persistent Connection object to the database, initializing it if necessary.
     * @return A Connection object to the database.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        // Avoids null pointer or "closed" exceptions
        if (conn == null || conn.isClosed()) {

            // Define the resource locator for the database
            String url = "jdbc:h2:./EnergyDatabase";

            // Create and store the Connection instance
            conn = DriverManager.getConnection(url);
        }

        // Return the existing or newly created Connection
        return conn;
    }




    public void createEnergyRecordsTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS energyRecords (" +
                "time LOCALDATETIME PRIMARY KEY, " +
                "time_label STRING, " +
                "price_p_kWh FLOAT, " +
                "carbon_gCO2_kWh FLOAT, " +
                "total_load_kWh FLOAT, " +
                "solar_gen_kWh FLOAT, " +
                "battery_level_kWh FLOAT, " +
                "grid_import_kWh FLOAT, " +
                "ev_charger_kWh FLOAT, " +
                "washing_machine_kWh FLOAT, " +
                "heating_kWh FLOAT, " +
                "baseload_kWh FLOAT" +
                ")";
        getConnection().createStatement().execute(sql);
    }

    public void importFromCSV(String filePath) {
        // Assumes CSV headings match db table headings, so we should edit CSV if possible.
        String sql = "INSERT INTO energyRecords SELECT * FROM CSVREAD('" + filePath + "')";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


 /*

    public void fromSimulationService() {

    }


    private void addData(EnergyRecord record) {
        String sql = "INSERT INTO energyRecords (" +
                "time, time_label, price_p_kWh, carbon_gCO2_kWh, " +
                "total_load_kWh, solar_gen_kWh, battery_level_kWh, " +
                "grid_import_kWh, ev_charger_kWh, washing_machine_kWh, " +
                "heating_kWh, baseload_kWh" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {

            pstmt.setTimestamp(1, record.getTimestamp());
            //pstmt.setString(2, record.);
            pstmt.setDouble(3, record.getPricePerKwh());
            pstmt.setDouble(4, record.getCarbon());
            pstmt.setDouble(5, record.getTotalLoadKwh());
            pstmt.setDouble(6, record.getSolarGenKwh());
            pstmt.setDouble(7, record.getBatteryLevel());
            pstmt.setDouble(8, record.getGridImport());
            pstmt.setDouble(9, record.getEvChargerUsageKwh());
            pstmt.setDouble(10, record.getWashingMachineUsageKwh());
            pstmt.setDouble(11, record.getHeatingUsageKwh());
            pstmt.setDouble(12, record.getBaseloadKwh());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }

    }
*/


}