package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForecastController {

    @FXML
    private LineChart<Number, Number> energyCostForecastLineChart;

    @FXML
    private Button forecastingReload;

    @FXML
    private Label lastUpdatedLabel;

    private XYChart.Series<Number, Number> energyCostSeries = new XYChart.Series<>();

    private List<EnergyRecord> energyRecords;
    private List<EnergyRecord> forcastEnergyRecords;

    @FXML
    public void initialize() {
        energyCostForecastLineChart.getData().add(energyCostSeries);
    }

    public void setSimulationService (SimulationService simulationService) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            updateGraph(newVal);
            lastUpdatedLabel.setText(LocalTime.now().format(formatter));
        });
    }

    private void updateGraph(EnergyRecord newVal) {
        energyRecords.add(newVal);
        forcastEnergyRecords = predict(energyRecords);
        energyCostSeries.getData().clear();
        energyCostSeries = plotData();


    }

    private List<EnergyRecord> predict(List<EnergyRecord> energyRecords) {
        for (int i = 0; i < 48; i++){
            if (getTime(energyRecords.get(i).getTimestamp()) == (double) i /2) {

            }else{
                energyRecords.add(i,null);
            }
        }
        return energyRecords;
    }

    private double getTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH.mm");
        String time = dateTime.format(formatter);
        return Math.round(2*Float.parseFloat(time)) / 2.0;
    }

    private XYChart.Series<Number, Number> plotData() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (EnergyRecord energyRecord : forcastEnergyRecords) {
            XYChart.Data<Number, Number> energyData = null;
            if (energyRecord != null) {
                double x = getTime(energyRecord.getTimestamp());
                double y = energyRecord.getPricePerKwh();
                energyData = new XYChart.Data<>(x, y);
            }
            series.getData().add(energyData);
        }
        return series;
    }
}