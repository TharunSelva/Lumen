package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationService {

    private List<EnergyRecord> simulationData = new ArrayList<>();
    private final DateTimeFormatter csvDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private int currentDataIndex = 0;

    // ObjectProperty makes the currentEnergyRecord observable by any controllers listening.
    // controllers can react when simulation time updates.
    private final ObjectProperty<EnergyRecord> currentEnergyRecord = new SimpleObjectProperty<>();
    private ScheduledExecutorService scheduler;
    private double totalAccumulatedCost = 0.0;

    public void loadData(String filepath) {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filepath)))) {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                LocalDateTime timestamp = LocalDateTime.parse(values[0], csvDateFormatter);
                double pricePerKwh = Double.parseDouble(values[2]);
                double gridImport = Double.parseDouble(values[7]);
                EnergyRecord energyRecord = new EnergyRecord(timestamp, gridImport, pricePerKwh);
                simulationData.add(energyRecord);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startSimulation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::advanceSimulation, 0, 2, TimeUnit.SECONDS);
    }
    private final DoubleProperty currentIntervalCost = new SimpleDoubleProperty();
    private void advanceSimulation() {
        if (currentDataIndex < simulationData.size()) {
            EnergyRecord record = simulationData.get(currentDataIndex);
            double costOnThisInterval = (record.getGridImport() * record.getPricePerKwh()) / 100;
            totalAccumulatedCost += costOnThisInterval;
            // timer runs on a background thread. need to use runLater to safely do the update on the JavaFX main thread.
            Platform.runLater(() -> {
                currentEnergyRecord.set(record);
                currentIntervalCost.set(costOnThisInterval);
            });
            currentDataIndex++;
        }
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

    public DoubleProperty currentIntervalCostProperty() {
        return currentIntervalCost;
    }
}
