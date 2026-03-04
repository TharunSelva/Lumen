package com.energyapp.controller;

import com.energyapp.utils.EnergyCalculator;
import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.time.format.DateTimeFormatter;

public class EcoStatisticsController {

    @FXML private StackPane cleanEnergyChartContainer;
    @FXML private BarChart<String, Number> environmentalBreakdownChart;
    @FXML private Label cleanEnergyShareLabel;

    private LineChart<String, Number> cleanEnergyLineChart;
    private XYChart.Series<String, Number> cleanEnergySeries;

    private XYChart.Series<String, Number> solarBarSeries;
    private XYChart.Series<String, Number> gridBarSeries;

    private SimulationService simulationService;
    private double accumulatedSolar = 0.0;
    private double accumulatedGrid = 0.0;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        setupCleanEnergyLineChart();
        setupEnvironmentalBreakdownChart();
    }

    private void setupCleanEnergyLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20); 
        yAxis.setLabel("% Clean");

        cleanEnergyLineChart = new LineChart<>(xAxis, yAxis);
        cleanEnergyLineChart.setCreateSymbols(false); 
        cleanEnergyLineChart.setAnimated(false);
        cleanEnergyLineChart.setLegendVisible(false);

        cleanEnergySeries = new XYChart.Series<>();
        cleanEnergyLineChart.getData().add(cleanEnergySeries);

        cleanEnergyChartContainer.getChildren().clear();
        cleanEnergyChartContainer.getChildren().add(cleanEnergyLineChart);
    }

    private void setupEnvironmentalBreakdownChart() {
        environmentalBreakdownChart.setAnimated(false);
        
        solarBarSeries = new XYChart.Series<>();
        solarBarSeries.setName("Solar (Clean)");
        
        gridBarSeries = new XYChart.Series<>();
        gridBarSeries.setName("Grid/Fossil");

        environmentalBreakdownChart.getData().addAll(solarBarSeries, gridBarSeries);
    }

    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
        populateFromHistory();
        this.simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) Platform.runLater(() -> updateCharts(newVal));
        });
    }

    private void populateFromHistory() {
        accumulatedSolar = 0.0;
        accumulatedGrid = 0.0;
        cleanEnergySeries.getData().clear();
        solarBarSeries.getData().clear();
        gridBarSeries.getData().clear();

        for (EnergyRecord record : simulationService.getEnergyStatsSessionHistory()) {
            updateCharts(record);
        }
    }

    private void updateCharts(EnergyRecord record) {
        String timeStr = record.getTimestamp().format(timeFormatter);
        double solar = record.getSolarGenKwh();
        double grid = record.getGridImport();
        double totalLoad = solar + grid;

        double cleanSharePercent = EnergyCalculator.calculateCleanShare(totalLoad, solar);
        cleanEnergySeries.getData().add(new XYChart.Data<>(timeStr, cleanSharePercent));

        if (cleanEnergySeries.getData().size() > 20) {
            cleanEnergySeries.getData().remove(0);
        }

        cleanEnergyShareLabel.setText(String.format("Clean Energy Share: %.0f%%", cleanSharePercent));

        accumulatedSolar += solar;
        accumulatedGrid += grid;

        if (solarBarSeries.getData().isEmpty()) {
            solarBarSeries.getData().add(new XYChart.Data<>("Total kWh", accumulatedSolar));
            gridBarSeries.getData().add(new XYChart.Data<>("Total kWh", accumulatedGrid));
        } else {
            solarBarSeries.getData().get(0).setYValue(accumulatedSolar);
            gridBarSeries.getData().get(0).setYValue(accumulatedGrid);
        }
    }
}