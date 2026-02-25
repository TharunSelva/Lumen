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


    private void addData(EnergyRecord record) {

    }


    public void createEnergyRecordsTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS energyRecords (" +
                "time REAL PRIMARY KEY, s" +
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


    public void fromSimulationService() {

    }


}