package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForecastController {

    @FXML
    private LineChart<?, ?> energyCostForecastLineChart;

    @FXML
    private Button forecastingReload;

    @FXML
    private Label lastUpdatedLabel;

    private List<EnergyRecord> energyRecords;

    public void initialize() {
        SimulationService
    }

    public setSimulationService (SimulationService simulationService) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            updateGraph(newVal);
            lastUpdatedLabel.setText(LocalTime.now().format(formatter));
        });
    }

    private void updateGraph(EnergyRecord newVal) {
        energyRecords.add(newVal);
    }

}