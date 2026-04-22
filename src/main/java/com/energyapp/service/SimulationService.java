package com.energyapp.service;
import com.energyapp.model.EnergyRecord;
import com.energyapp.utils.CsvReadForService;
import com.energyapp.utils.EnergyCalculator;
import com.energyapp.db.EnergyIntervalRepository;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SimulationService {
    private List<EnergyRecord> simulationData = new ArrayList<>();
    private int currentDataIndex = 0;
    private List<EnergyRecord> currentData;
    public SimulationService() {
        this.currentData = new ArrayList<>();
        this.currentData = CsvReadForService.loadData();

        if (this.currentData == null) {
            this.currentData = new ArrayList<>();
            System.out.println("Warning: CSV failed to load, initializing empty list.");
        }
    }

    // ObjectProperty makes the currentEnergyRecord observable by any controllers listening.
    // controllers can react when simulation time updates.
    private final ObjectProperty<EnergyRecord> currentEnergyRecord = new SimpleObjectProperty<>();
    private ScheduledExecutorService scheduler;
    private double totalAccumulatedCost = 0.0;
    private double weeklyAccumulatedCost = 0.0;
    private final List<EnergyRecord> energyStatsSessionHistory = new ArrayList<>();

    private final EnergyIntervalRepository repo = new EnergyIntervalRepository();
    private static final String HOME_ID = "HOME_1";

    public void loadData() {
//        simulationData = CsvReadForService.loadData();

        // Replace CSV loading DB loading
        simulationData = repo.fetchAll(HOME_ID);

        if (simulationData.isEmpty()) {
            throw new IllegalStateException(
                    "No simulation data found in DB. Did DbBootstrap import simulation_data.csv into energy_interval?"
            );
        }
    }

    public void startSimulation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::advanceSimulation, 0, 2, TimeUnit.SECONDS);
    }
    private final DoubleProperty currentIntervalCost = new SimpleDoubleProperty();
    private final DoubleProperty weeklyAccumulatedCostProperty = new SimpleDoubleProperty();
    private int daysElapsed = 0;
    private double weeklyEvKwh = 0, weeklyWashKwh = 0, weeklyHeatKwh = 0, weeklyBaseKwh = 0, weeklyOtherKwh = 0;
    private void advanceSimulation() {
        if (currentDataIndex < simulationData.size()) {
            if (currentDataIndex > 0 && currentDataIndex % 336 == 0) {
                System.out.println("Week boundary reached. Starting a new simulated week.");
                resetWeeklyCounters();
            }

            EnergyRecord record = simulationData.get(currentDataIndex);
            weeklyEvKwh += record.getEvChargerUsageKwh();
            weeklyWashKwh += record.getWashingMachineUsageKwh();
            weeklyHeatKwh += record.getHeatingUsageKwh();
            weeklyBaseKwh += record.getBaseloadKwh();
            weeklyOtherKwh += (record.getBaseloadKwh() * 0.1);
            double costOnThisInterval = EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh());
            totalAccumulatedCost += costOnThisInterval;
            weeklyAccumulatedCost += costOnThisInterval;

            synchronized (energyStatsSessionHistory) {
                energyStatsSessionHistory.add(record);
            }

            // timer runs on a background thread. need to use runLater to safely do the update on the JavaFX main thread.
            Platform.runLater(() -> {
                currentEnergyRecord.set(record);
                currentIntervalCost.set(costOnThisInterval);
                weeklyAccumulatedCostProperty.set(weeklyAccumulatedCost);
            });
            currentDataIndex++;

        }else{
            currentDataIndex = 0;
            resetWeeklyCounters();
        }
    }

    private void resetWeeklyCounters() {
        daysElapsed = 0;
        weeklyEvKwh = 0;
        weeklyWashKwh = 0;
        weeklyHeatKwh = 0;
        weeklyBaseKwh = 0;
        weeklyOtherKwh = 0;
        weeklyAccumulatedCost = 0;
        Platform.runLater(() -> weeklyAccumulatedCostProperty.set(0));
        System.out.println("New Week Started: Data Reset.");
    }

    public Map<String, Double> getWeeklyAccumulatedUsage() {
        Map<String, Double> totals = new HashMap<>();
        totals.put("EV Charger", weeklyEvKwh);
        totals.put("Washing Machine", weeklyWashKwh);
        totals.put("Heating", weeklyHeatKwh);
        totals.put("Baseload", weeklyBaseKwh);
        totals.put("Lighting & Misc", weeklyOtherKwh);
        return totals;
    }

    public ObjectProperty<EnergyRecord> getCurrentEnergyRecord() {
        return currentEnergyRecord;
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("Simulation stopped.");
        }
    }

    public double getTotalAccumulatedCost() {
        return totalAccumulatedCost;
    }

    public double getWeeklyAccumulatedCost() {
        return weeklyAccumulatedCost;
    }

    public List<EnergyRecord> getEnergyStatsSessionHistory() {
        synchronized (energyStatsSessionHistory) {
            return new ArrayList<>(energyStatsSessionHistory);
        }
    }

    public DoubleProperty currentIntervalCostProperty() {
        return currentIntervalCost;
    }

    public ReadOnlyDoubleProperty weeklyAccumulatedCostProperty() {
        return weeklyAccumulatedCostProperty;
    }

    private Connection getDbConnection() {
        String dbPath = System.getProperty("user.home") + "/.energyapp/data/energy_tool.sqlite";
        String url = "jdbc:sqlite:" + dbPath;
        try{
            return DriverManager.getConnection(url);
        } catch (SQLException e){
            System.err.println("Error connecting to database." + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Gets grid fuel percentage for the current simulation step (cycles through different time intervals)
     */
    public double getGridFuelPercentageForStep(String fuelType, int step) {
        String sql = """
            SELECT perc
            FROM grid_generation_mix_interval
            WHERE LOWER(fuel) = LOWER(?)
            ORDER BY ts_start DESC
            LIMIT 1 OFFSET ?
            """;

        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fuelType);
            stmt.setInt(2, step % 8);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double perc = rs.getDouble("perc");
                System.out.println("DEBUG: Grid " + fuelType + " (step " + step + ") = " + perc + "%");
                return perc;
            }
        } catch (Exception e) {
            System.err.println("Error fetching grid fuel '" + fuelType + "': " + e.getMessage());
        }
        return 0.0;
    }

    public int getCurrentDataIndex() {
        return currentDataIndex;
    }

    public List<EnergyRecord> getUpcomingRecords(int limit) {
        if (simulationData == null || simulationData.isEmpty()) {
            return Collections.emptyList();
        }
        int end = Math.min(currentDataIndex + limit, simulationData.size());
        if (currentDataIndex >= end) {
            return Collections.emptyList();
        }
        return new ArrayList<>(simulationData.subList(currentDataIndex, end));
    }
}
