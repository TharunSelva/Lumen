package com.energyapp.controller;

import com.energyapp.utils.EnergyCalculator;
import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.time.format.DateTimeFormatter;
import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class EcoStatisticsController {

    @FXML private StackPane cleanEnergyChartContainer;
    @FXML private Label cleanEnergyShareLabel;
    @FXML private Label carbonSavedLabel;
    @FXML private Label carbonFootprintLabel;
    @FXML private BarChart<Number, String> personalSourcesChart;
    @FXML private BarChart<Number, String> gridSourcesChart;
    private double accumulatedGridCarbon = 0.0;
    private double accumulatedSolarCarbonSaved = 0.0;

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
//        environmentalBreakdownChart.setAnimated(false);
        
        solarBarSeries = new XYChart.Series<>();
        solarBarSeries.setName("Solar (Clean)");
        
        gridBarSeries = new XYChart.Series<>();
        gridBarSeries.setName("Grid/Fossil");

//        environmentalBreakdownChart.getData().addAll(solarBarSeries, gridBarSeries);
    }

    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
        populateFromHistory();
        loadEnergySourceBreakdown();
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(this::loadEnergySourceBreakdown);
        });
        this.simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) Platform.runLater(() -> updateCharts(newVal));
        });
    }

    private void populateFromHistory() {
        accumulatedSolar = 0.0;
        accumulatedGrid = 0.0;
        accumulatedGridCarbon = 0.0;
        accumulatedSolarCarbonSaved = 0.0;
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

        double currentGridIntensity = record.getCarbon();

        accumulatedGridCarbon += (grid * currentGridIntensity);
        accumulatedSolarCarbonSaved += (solar * currentGridIntensity);

        double avgFootprint = (accumulatedGrid > 0) ? (accumulatedGridCarbon / accumulatedGrid) : 0.0;

        double savedKg = accumulatedSolarCarbonSaved / 1000.0;

        carbonFootprintLabel.setText(String.format("%.0f gCO₂/kWh", avgFootprint));
        carbonSavedLabel.setText(String.format("%.2f kg CO₂", savedKg));

        if (solarBarSeries.getData().isEmpty()) {
            solarBarSeries.getData().add(new XYChart.Data<>("Total kWh", accumulatedSolar));
            gridBarSeries.getData().add(new XYChart.Data<>("Total kWh", accumulatedGrid));
        } else {
            solarBarSeries.getData().get(0).setYValue(accumulatedSolar);
            gridBarSeries.getData().get(0).setYValue(accumulatedGrid);
        }
    }
    private void loadEnergySourceBreakdown() {
        loadPersonalSourcesChart();
        loadGridSourcesChart();
    }

    private void loadPersonalSourcesChart() {
        if (personalSourcesChart == null) return;

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Personal Sources");

        EnergyRecord current = simulationService != null ? simulationService.getCurrentEnergyRecord().get() : null;

        double solarPersonal = 0.0;
        double batteryPersonal = 0.0;

        if (current != null) {
            solarPersonal = EnergyCalculator.calculateSolarUsed(
                    current.getTotalLoadKwh(), current.getSolarGenKwh());

            batteryPersonal = EnergyCalculator.calculateBatteryUsed(
                    current.getTotalLoadKwh(), current.getSolarGenKwh(), current.getGridImport());
        }

        String[] categories = {"Solar (Personal)", "Wind (Personal)", "Battery (Personal)"};
        double[] values     = {solarPersonal, 0.0, batteryPersonal};

        for (int i = 0; i < categories.length; i++) {
            series.getData().add(new XYChart.Data<>(values[i], categories[i]));
        }

        personalSourcesChart.getData().clear();
        personalSourcesChart.getData().add(series);
        CategoryAxis yAxis = (CategoryAxis) personalSourcesChart.getYAxis();
        yAxis.setCategories(FXCollections.observableArrayList(categories));
        personalSourcesChart.requestLayout();
    }

    private void loadGridSourcesChart() {
        if (gridSourcesChart == null) return;
        int step = simulationService.getCurrentDataIndex();

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Grid Sources");

        double solarGrid = simulationService.getGridFuelPercentageForStep("solar", step);
        double windGrid  = simulationService.getGridFuelPercentageForStep("wind", step);
        double hydroGrid = simulationService.getGridFuelPercentageForStep("hydro", step);

        double biomassGrid = simulationService.getGridFuelPercentageForStep("biomass",step);
        double gasGrid     = simulationService.getGridFuelPercentageForStep("gas",step);
        double coalGrid    = simulationService.getGridFuelPercentageForStep("coal",step);
        double fossilGrid  = biomassGrid + gasGrid + coalGrid;

        String[] categories = {"Solar (Grid)", "Wind (Grid)", "Fossil (Grid)", "Hydroelectric (Grid)"};
        double[] values     = {solarGrid, windGrid, fossilGrid, hydroGrid};

        for (int i = 0; i < categories.length; i++) {
            series.getData().add(new XYChart.Data<>(values[i], categories[i]));
        }

        gridSourcesChart.getData().clear();
        gridSourcesChart.getData().add(series);
        CategoryAxis yAxis = (CategoryAxis) gridSourcesChart.getYAxis();
        yAxis.setCategories(FXCollections.observableArrayList(categories));
        gridSourcesChart.requestLayout();
        gridSourcesChart.layout();
        System.out.println("DEBUG: Grid chart visually updated at step " + step);
    }
}