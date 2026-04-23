package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class EcoStatisticsController {

    static final List<String> PERSONAL_SOURCE_NAMES = List.of("Grid", "Battery", "Solar");
    static final List<String> GRID_FUEL_NAMES = List.of(
            "biomass", "coal", "imports", "gas", "nuclear", "other", "hydro", "solar", "wind"
    );

    @FXML private StackPane cleanEnergyChartContainer;
    @FXML private Label cleanEnergyShareLabel;
    @FXML private Label carbonSavedLabel;
    @FXML private Label carbonFootprintLabel;
    @FXML private BarChart<Number, String> personalSourcesChart;
    @FXML private BarChart<Number, String> gridSourcesChart;
    @FXML private Label currentHourStatusLabel;
    @FXML private Label currentGridIntensityLabel;
    @FXML private Label carbonRecommendationLabel;
    @FXML private BarChart<String, Number> carbonHistoryChart;

    private double accumulatedGridCarbon = 0.0;
    private double accumulatedSolarCarbonSaved = 0.0;
    private double accumulatedSolar = 0.0;
    private double accumulatedGrid = 0.0;

    private LineChart<String, Number> cleanEnergyLineChart;
    private XYChart.Series<String, Number> cleanEnergySeries;
    private XYChart.Series<String, Number> todayCarbonSeries;

    private SimulationService simulationService;
    private int currentDayOfYear = -1;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        setupCleanEnergyLineChart();
        setupEnvironmentalBreakdownCharts();
        setupCarbonHistoryChart();
    }

    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
        populateFromHistory();
        loadEnergySourceBreakdown();

        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> {
                    updateCharts(newVal);
                    loadEnergySourceBreakdown();
                });
            }
        });
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

    private void setupEnvironmentalBreakdownCharts() {
        if (personalSourcesChart != null) {
            personalSourcesChart.setAnimated(false);
            personalSourcesChart.setLegendVisible(false);
        }
        if (gridSourcesChart != null) {
            gridSourcesChart.setAnimated(false);
            gridSourcesChart.setLegendVisible(false);
        }
    }

    private void populateFromHistory() {
        accumulatedSolar = 0.0;
        accumulatedGrid = 0.0;
        accumulatedGridCarbon = 0.0;
        accumulatedSolarCarbonSaved = 0.0;
        cleanEnergySeries.getData().clear();

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

        carbonFootprintLabel.setText(String.format("%.0f gCO2/kWh", avgFootprint));
        carbonSavedLabel.setText(String.format("%.2f kg CO2", savedKg));

        HourStatus hourStatus = determineHourStatus(currentGridIntensity);
        String timeStrOnlyHour = record.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (currentHourStatusLabel != null) {
            currentHourStatusLabel.setText(timeStrOnlyHour + " - " + hourStatus.status());
            currentGridIntensityLabel.setText(String.format("Grid intensity: %.0f gCO2/kWh", currentGridIntensity));
            carbonRecommendationLabel.setText(hourStatus.recommendation());
            setCurrentHourStatusStyle(hourStatus.styleClass());
        }

        int recordDayOfYear = record.getTimestamp().getDayOfYear();
        if (currentDayOfYear != recordDayOfYear) {
            currentDayOfYear = recordDayOfYear;
            loadPredictedEcoImpactForDay(record.getTimestamp().toLocalDate());
        }
    }

    private void loadEnergySourceBreakdown() {
        loadPersonalSourcesChart();
        loadGridSourcesChart();
    }

    private void loadPersonalSourcesChart() {
        if (personalSourcesChart == null) {
            return;
        }

        renderHorizontalBarChart(
                personalSourcesChart,
                "Personal Sources",
                buildPersonalSources(getLatestAvailableRecord()),
                List.of("#3b82f6", "#10b981", "#f59e0b")
        );
    }

    private void loadGridSourcesChart() {
        if (gridSourcesChart == null || simulationService == null) {
            return;
        }

        int step = simulationService.getCurrentDataIndex();
        renderHorizontalBarChart(
                gridSourcesChart,
                "Grid Sources",
                buildGridSources(fuel -> simulationService.getGridFuelPercentageForStep(fuel, step)),
                List.of("#65a30d", "#334155", "#64748b", "#f97316", "#8b5cf6", "#94a3b8", "#06b6d4", "#f59e0b", "#22c55e")
        );
    }

    private void renderHorizontalBarChart(
            BarChart<Number, String> chart,
            String seriesName,
            List<SourceRow> rows,
            List<String> colors
    ) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (SourceRow row : rows) {
            series.getData().add(new XYChart.Data<>(row.value(), row.name()));
        }

        chart.getData().setAll(series);

        CategoryAxis yAxis = (CategoryAxis) chart.getYAxis();
        yAxis.setCategories(FXCollections.observableArrayList(rows.stream().map(SourceRow::name).toList()));

        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<Number, String> data = series.getData().get(i);
            String color = colors.get(i % colors.size());
            data.nodeProperty().addListener((obs, oldNode, newNode) -> styleSourceBar(newNode, color, data));
            styleSourceBar(data.getNode(), color, data);
        }

        chart.requestLayout();
    }

    private void styleSourceBar(Node node, String color, XYChart.Data<Number, String> data) {
        if (node == null) {
            return;
        }

        node.setStyle("-fx-bar-fill: " + color + ";");
        Tooltip tooltip = new Tooltip(String.format("%s: %.2f", data.getYValue(), data.getXValue().doubleValue()));
        tooltip.setShowDelay(Duration.millis(100));
        Tooltip.install(node, tooltip);
    }

    static List<SourceRow> buildPersonalSources(EnergyRecord current) {
        double solarPersonal = 0.0;
        double batteryPersonal = 0.0;
        double gridPersonal = 0.0;

        if (current != null) {
            double totalLoad = Math.max(current.getTotalLoadKwh(), 0.0);
            solarPersonal = Math.min(totalLoad, Math.max(current.getSolarGenKwh(), 0.0));
            double remainingAfterSolar = Math.max(totalLoad - solarPersonal, 0.0);
            gridPersonal = Math.min(Math.max(current.getGridImport(), 0.0), remainingAfterSolar);
            batteryPersonal = Math.max(totalLoad - solarPersonal - gridPersonal, 0.0);
        }

        return List.of(
                new SourceRow("Grid", gridPersonal),
                new SourceRow("Battery", batteryPersonal),
                new SourceRow("Solar", solarPersonal)
        );
    }

    static List<SourceRow> buildGridSources(Function<String, Double> fuelLookup) {
        return GRID_FUEL_NAMES.stream()
                .map(fuel -> new SourceRow(toDisplayFuelName(fuel), safeLookup(fuelLookup, fuel)))
                .toList();
    }

    static HourStatus determineHourStatus(double currentCarbon) {
        if (currentCarbon < 100) {
            return new HourStatus(
                    "Low carbon hour",
                    "Great time for flexible tasks like laundry or EV charging",
                    "eco-hour-low"
            );
        }
        if (currentCarbon < 200) {
            return new HourStatus(
                    "Moderate carbon hour",
                    "Okay to use energy, but shift heavy loads if possible",
                    "eco-hour-moderate"
            );
        }
        return new HourStatus(
                "High carbon hour",
                "Avoid heavy energy usage right now",
                "eco-hour-high"
        );
    }

    private static double safeLookup(Function<String, Double> fuelLookup, String fuel) {
        Double value = fuelLookup.apply(fuel);
        return value == null ? 0.0 : value;
    }

    private static String toDisplayFuelName(String fuel) {
        if (fuel == null || fuel.isBlank()) {
            return "Other";
        }
        return fuel.substring(0, 1).toUpperCase(Locale.ROOT) + fuel.substring(1).toLowerCase(Locale.ROOT);
    }

    private EnergyRecord getLatestAvailableRecord() {
        if (simulationService == null) {
            return null;
        }

        EnergyRecord current = simulationService.getCurrentEnergyRecord().get();
        if (current != null) {
            return current;
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    private void setCurrentHourStatusStyle(String modifierClass) {
        currentHourStatusLabel.getStyleClass().removeAll("eco-hour-low", "eco-hour-moderate", "eco-hour-high");
        if (!currentHourStatusLabel.getStyleClass().contains("eco-hour-pill")) {
            currentHourStatusLabel.getStyleClass().add("eco-hour-pill");
        }
        currentHourStatusLabel.getStyleClass().add(modifierClass);
    }

    private void setupCarbonHistoryChart() {
        todayCarbonSeries = new XYChart.Series<>();
        todayCarbonSeries.setName("Grid Carbon Intensity");

        if (carbonHistoryChart != null) {
            carbonHistoryChart.getData().add(todayCarbonSeries);
            carbonHistoryChart.setAnimated(false);
        }
    }

    private void loadPredictedEcoImpactForDay(LocalDate date) {
        if (simulationService == null || carbonHistoryChart == null || todayCarbonSeries == null) {
            return;
        }

        List<EnergyRecord> dailyRecords = simulationService.getRecordsForDate(date);
        todayCarbonSeries.getData().clear();

        double[] carbonSums = new double[24];
        int[] carbonCounts = new int[24];

        for (EnergyRecord record : dailyRecords) {
            int hour = record.getTimestamp().getHour();
            carbonSums[hour] += record.getCarbon();
            carbonCounts[hour]++;
        }

        for (int i = 0; i < 24; i++) {
            double avgCarbon = carbonCounts[i] > 0 ? (carbonSums[i] / carbonCounts[i]) : 0.0;
            String hourLabel = String.format("%02d:00", i);

            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(hourLabel, avgCarbon);
            dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    String tooltipText = String.format("Time: %s\nPredicted Intensity: %.0f gCO2/kWh", hourLabel, avgCarbon);
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setShowDelay(Duration.millis(100));
                    Tooltip.install(newNode, tooltip);

                    if (avgCarbon < 100) {
                        newNode.setStyle("-fx-bar-fill: #10B981;");
                    } else if (avgCarbon < 200) {
                        newNode.setStyle("-fx-bar-fill: #F59E0B;");
                    } else {
                        newNode.setStyle("-fx-bar-fill: #EF4444;");
                    }
                }
            });

            todayCarbonSeries.getData().add(dataPoint);
        }
    }

    record SourceRow(String name, double value) { }

    record HourStatus(String status, String recommendation, String styleClass) { }
}
