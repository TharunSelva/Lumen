package com.energyapp.service;

import java.sql.*;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;


public class DatabaseService {
    private static Connection conn = null;
    SimulationService simulationService;

    public DatabaseService() {

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

    public boolean checkTable(String tableName) throws SQLException {
        DatabaseMetaData dbmd = getConnection().getMetaData();
        try (ResultSet rs = dbmd.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    public createEnergyRecordsTable () throws SQLException {
        if (checkTable("energyRecords")){
            return;
        }
        else{
            String sql = "CREATE TABLE energyRecords (" +
                    "id  PRIMARY KEY, " +
                    "2 DATATYPE, " +
                    "3 DATATYPE, " +
                    "4 DATATYPE" +
                    ")";
        }
    }


}