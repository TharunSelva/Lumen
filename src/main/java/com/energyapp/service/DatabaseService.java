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

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            String url = "jdbc:h2:./EnergyRecords";
            conn = DriverManager.getConnection(url);
        }
        return conn;
    }


    private void addData(EnergyRecord record) {

    }


}