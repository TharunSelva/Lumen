package com.energyapp.service;

import java.sql.*;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;


public class DatabaseService {
    Connection conn = null;
    SimulationService simulationService;

    public DatabaseService() {
        String url = "jbdc:sqlite::memory:";
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {}
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }



        simulationService = new SimulationService();
        simulationService.startSimulation();
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                addData(newVal);
            }
        });

    }

    public Connection getConnection() {
        return conn;
    }


    private void addData(EnergyRecord record) {

    }

}