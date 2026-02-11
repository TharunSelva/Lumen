package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;


public class ForecastController {

    @FXML
    private LineChart<?, ?> energyCostForecastLineChart;

    @FXML
    private Button forecastingReload;

    @FXML
    private Label lastUpdatedLabel;



    public void initialize() {
        SimulationService
    }

    public setSimulationService (SimulationService simulationService) {
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            updateGraph(newVal);
            lastUpdatedLabel.setText();
        });
    }

    private void updateGraph(EnergyRecord newVal) {

    }

}