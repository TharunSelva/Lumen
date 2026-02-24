package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import com.energyapp.utils.CsvReadForService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
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
    private final List<EnergyRecord> energyStatsSessionHistory = new ArrayList<>();

    public void loadData() {
        simulationData = CsvReadForService.loadData();
    }

    public void startSimulation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::advanceSimulation, 0, 2, TimeUnit.SECONDS);
    }
    private final DoubleProperty currentIntervalCost = new SimpleDoubleProperty();
    private int daysElapsed = 0;
    private double weeklyEvKwh = 0, weeklyWashKwh = 0, weeklyHeatKwh = 0, weeklyBaseKwh = 0;
    private void advanceSimulation() {
        if (currentDataIndex < simulationData.size()) {
            EnergyRecord record = simulationData.get(currentDataIndex);
            weeklyEvKwh += record.getEvChargerUsageKwh();
            weeklyWashKwh += record.getWashingMachineUsageKwh();
            weeklyHeatKwh += record.getHeatingUsageKwh();
            weeklyBaseKwh += record.getBaseloadKwh();
            double costOnThisInterval = EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh());
            totalAccumulatedCost += costOnThisInterval;

            if (currentDataIndex > 0 && currentDataIndex % 336 == 0) {
                System.out.println("Week 1 Finished. Resetting Breakdown for Week 2.");
                resetWeeklyCounters();
            }

            synchronized (energyStatsSessionHistory) {
                energyStatsSessionHistory.add(record);
            }

            // timer runs on a background thread. need to use runLater to safely do the update on the JavaFX main thread.
            Platform.runLater(() -> {
                currentEnergyRecord.set(record);
                currentIntervalCost.set(costOnThisInterval);
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
        System.out.println("New Week Started: Data Reset.");
    }

    public Map<String, Double> getWeeklyAccumulatedUsage() {
        Map<String, Double> totals = new HashMap<>();
        totals.put("EV Charger", weeklyEvKwh);
        totals.put("Washing Machine", weeklyWashKwh);
        totals.put("Heating", weeklyHeatKwh);
        totals.put("Baseload", weeklyBaseKwh);
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

    public List<EnergyRecord> getEnergyStatsSessionHistory() {
        synchronized (energyStatsSessionHistory) {
            return new ArrayList<>(energyStatsSessionHistory);
        }
    }

    public Map<String, Double> getDeviceTotals() {
        if (currentData == null || currentData.isEmpty()) {
            Map<String, Double> emptyMap = new HashMap<>();
            emptyMap.put("EV Charger", 0.0);
            emptyMap.put("Washing Machine", 0.0);
            emptyMap.put("Heating", 0.0);
            emptyMap.put("Baseload", 0.0);
            return emptyMap;
        }

        double ev = 0, wash = 0, heat = 0, base = 0;
        for (EnergyRecord record : currentData) {
            ev += record.getEvChargerUsageKwh();
            wash += record.getWashingMachineUsageKwh();
            heat += record.getHeatingUsageKwh();
            base += record.getBaseloadKwh();
        }

        Map<String, Double> totals = new HashMap<>();
        totals.put("EV Charger", ev);
        totals.put("Washing Machine", wash);
        totals.put("Heating", heat);
        totals.put("Baseload", base);
        return totals;
    }

    public Map<String, Double> getLiveDeviceTotals(EnergyRecord currentRecord) {
        double ev = 0, wash = 0, heat = 0, base = 0;

        for (EnergyRecord record : simulationData) {
            // Add values to the sum
            ev += record.getEvChargerUsageKwh();
            wash += record.getWashingMachineUsageKwh();
            heat += record.getHeatingUsageKwh();
            base += record.getBaseloadKwh();

            // If this record is the one currently being plotted, STOP here
            if (record.equals(currentRecord)) {
                break;
            }
        }

        Map<String, Double> totals = new HashMap<>();
        totals.put("EV Charger", ev);
        totals.put("Washing Machine", wash);
        totals.put("Heating", heat);
        totals.put("Baseload", base);
        return totals;
    }

    public Map<String, Double> getWeeklyProjections(EnergyRecord currentRec) {
        double evCost = 0, washCost = 0, heatCost = 0, baseCost = 0;

        for (EnergyRecord record : simulationData) {
            double priceInPounds = record.getPricePerKwh() / 100.0;

            evCost += record.getEvChargerUsageKwh() * priceInPounds;
            washCost += record.getWashingMachineUsageKwh() * priceInPounds;
            heatCost += record.getHeatingUsageKwh() * priceInPounds;
            baseCost += record.getBaseloadKwh() * priceInPounds;

            if (record.equals(currentRec)) break;
        }

        Map<String, Double> weeklyProjections = new HashMap<>();
        weeklyProjections.put("EV Charger", evCost * 7);
        weeklyProjections.put("Washing Machine", washCost * 7);
        weeklyProjections.put("Heating", heatCost * 7);
        weeklyProjections.put("Baseload", baseCost * 7);

        return weeklyProjections;
    }

    public DoubleProperty currentIntervalCostProperty() {
        return currentIntervalCost;
    }
}
