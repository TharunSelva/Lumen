package com.energyapp.controller;

import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import com.energyapp.utils.DateTimeUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import java.time.*;
import java.util.List;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;

public class EnergyStatsController {

    @FXML private LineChart<Number, Number> usageChartSmall;
    @FXML private NumberAxis usageSmallXAxis;
    @FXML private NumberAxis usageSmallYAxis;

    @FXML private AnchorPane graphOverlay;
    @FXML private Button backButton;
    @FXML private ComboBox<String> timeRangeCombo;

    @FXML private LineChart<Number, Number> usageChartLarge;
    @FXML private NumberAxis usageLargeXAxis;
    @FXML private NumberAxis usageLargeYAxis;
    @FXML private BarChart<String, Number> costsChart;
    @FXML private CategoryAxis costsXAxis;
    @FXML private NumberAxis costsYAxis;
    @FXML private Label totalCostTodayLabel;
    @FXML private Label projectedDailyCostLabel;

    @FXML private SolarStatusCardController solarStatusCardController;

    @FXML private VBox device1;
    @FXML private VBox device2;
    @FXML private VBox device3;

    @FXML private Label device1holder, device2holder, device3holder;
    @FXML private Label device1useage, device2useage, device3useage;
    @FXML private Label weeklycost1, weeklycost2, weeklycost3;
    @FXML private Label page1, page2, page3;
    @FXML private BarChart<Number, String> personalSourcesChart;
    @FXML private BarChart<Number, String> gridSourcesChart;

    private List<DeviceData> allDevices = new ArrayList<>();
    private int currentPage = 0;

    private SimulationService simulationService;

    private final XYChart.Series<Number, Number> smallSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> largeSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> costsSeries = new XYChart.Series<>();

    // window: last hour default, -1 means "all time"
    private long windowSeconds = 60 * 60;
    private long latestEpochSec = 0;

    private enum ViewMode { REAL_TIME, LAST_1_HOUR, LAST_24_HOURS, ALL_TIME }
    private ViewMode viewMode = ViewMode.REAL_TIME;

    @FXML
    public void initialize() {
        page1.setOnMouseClicked(e -> { currentPage = 0; renderPage(); });
        page2.setOnMouseClicked(e -> { currentPage = 1; renderPage(); });
        page3.setOnMouseClicked(e -> { currentPage = 2; renderPage(); });

        // overlay toggling
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
        usageChartSmall.setOnMouseClicked(e -> showOverlay());
        backButton.setOnAction(e -> hideOverlay());

        // series
        usageChartSmall.getData().add(smallSeries);
        usageChartLarge.getData().add(largeSeries);
        costsChart.getData().add(costsSeries);
        costsChart.setLegendVisible(false);
        costsXAxis.setLabel("");
        costsYAxis.setLabel("");

        // axis formatting
        setupTimeAxis(usageSmallXAxis);
        usageSmallXAxis.setLabel("Time");
        usageSmallYAxis.setLabel("Power (kWh)");
        usageSmallYAxis.setAutoRanging(true);

        setupTimeAxis(usageLargeXAxis);
        usageLargeXAxis.setLabel("Time");
        usageLargeYAxis.setLabel("Power (kWh)");
        usageLargeYAxis.setAutoRanging(true);

        // overlay range controls
        // Time range dropdown
        timeRangeCombo.getItems().setAll(
                "Real time",
                "Last 1 hour",
                "Last 24 hours",
                "All time"
        );
        timeRangeCombo.getSelectionModel().select("Real time");
        timeRangeCombo.setOnAction(e -> onTimeRangeChanged());
    }

    // Handler for switching between the dropdown menu options
    private void onTimeRangeChanged() {
        String sel = timeRangeCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        switch (sel) {
            case "Real time" -> {
                viewMode = ViewMode.REAL_TIME;
                windowSeconds = 60 * 60;
            }
            case "Last 1 hour" -> {
                viewMode = ViewMode.LAST_1_HOUR;
                windowSeconds = 60 * 60;
            }
            case "Last 24 hours" -> {
                viewMode = ViewMode.LAST_24_HOURS;
                windowSeconds = 24 * 60 * 60;
            }
            case "All time" -> {
                viewMode = ViewMode.ALL_TIME;
                windowSeconds = -1; // special: all time
            }
        }

        // Apply the selected view to the charts
        applyViewToCharts();
    }

    private void applyViewToCharts() {
        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        if (history.isEmpty()) return;

        long end = DateTimeUtils.toEpochSeconds(history.get(history.size() - 1).getTimestamp());

        long start;
        if (windowSeconds < 0) {
            start = DateTimeUtils.toEpochSeconds(history.get(0).getTimestamp());
        } else {
            start = end - windowSeconds;
        }

        rebuildSeriesForRange(start, end);

        usageSmallXAxis.setLowerBound(start);
        usageSmallXAxis.setUpperBound(end);

        usageLargeXAxis.setLowerBound(start);
        usageLargeXAxis.setUpperBound(end);

    }

    private void rebuildSeriesForRange(long startEpochSec, long endEpochSec) {
        smallSeries.getData().clear();
        largeSeries.getData().clear();

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();

        for (EnergyRecord rec : history) {
            long t = DateTimeUtils.toEpochSeconds(rec.getTimestamp());

            if (t < startEpochSec) continue;
            if (t > endEpochSec) break;

            XYChart.Data<Number, Number> ds = new XYChart.Data<>(t, rec.getTotalLoadKwh());
            XYChart.Data<Number, Number> dl = new XYChart.Data<>(t, rec.getTotalLoadKwh());

            smallSeries.getData().add(ds);
            largeSeries.getData().add(dl);

            installTooltipWhenNodeReady(ds, rec);
            installTooltipWhenNodeReady(dl, rec);
        }
    }

    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setLabel("Time");
        axis.setForceZeroInRange(false);
        axis.setTickUnit(10 * 60);

        axis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number object) {
                return DateTimeUtils.formatTimeFromEpoch(object.longValue());
            }
            @Override public Number fromString(String string) { return 0; }
        });
    }

    private void installTooltipWhenNodeReady(XYChart.Data<Number, Number> d, EnergyRecord rec) {
        Platform.runLater(() -> {
            Node node = d.getNode();
            if (node == null) return;

            long epoch = DateTimeUtils.toEpochSeconds(rec.getTimestamp());
            String time = DateTimeUtils.formatTimeFromEpoch(epoch);

            // Cost (£) from totalLoadKwh and price (p/kWh)
            double costPounds = EnergyCalculator.calculateCostInPounds(rec.getGridImport(), rec.getPricePerKwh());

            double totalLoad = rec.getTotalLoadKwh();
            double solarUsed = EnergyCalculator.calculateSolarUsed(totalLoad, rec.getSolarGenKwh());
            double batteryUsed = EnergyCalculator.calculateBatteryUsed(totalLoad, rec.getSolarGenKwh(), rec.getGridImport());

            int solarPct = EnergyCalculator.calculatePercentage(solarUsed, totalLoad);
            int batteryPct = EnergyCalculator.calculatePercentage(batteryUsed, totalLoad);

            // Simple CO2 label
            String co2Label = EnergyCalculator.determineCo2Label(rec.getCarbon());

            // Tooltip text
            String tipText =
                    time + "\n" +
                            String.format("%.2f kWh • £%.2f • %d%%", rec.getTotalLoadKwh(), costPounds, solarPct) + "\n" +
                            String.format("Price: %.2f p/kWh • Total load: %.0f W", rec.getPricePerKwh(), rec.getWatts()) + "\n" +
                            String.format("Solar, %d%% Battery • %s", batteryPct, co2Label);

            Tooltip tip = new Tooltip(tipText);
            Tooltip.install(node, tip);

            // Click point -> open overlay
            node.setOnMouseClicked(ev -> showOverlay());
        });
    }

    private void refreshCostsChart() {
        if (simulationService == null) return;

        Runnable refreshAction = () -> rebuildCostsChartAndLabels(simulationService.getEnergyStatsSessionHistory());
        if (Platform.isFxApplicationThread()) {
            refreshAction.run();
        } else {
            Platform.runLater(refreshAction);
        }
    }

    private void rebuildCostsChartAndLabels(List<EnergyRecord> history) {
        if (history == null || history.isEmpty()) {
            costsSeries.getData().setAll();
            updateCostSummaryLabels(0.0, 0);
            return;
        }

        EnergyRecord latestRecord = history.stream()
                .max(Comparator.comparing(EnergyRecord::getTimestamp))
                .orElse(null);

        if (latestRecord == null) {
            costsSeries.getData().setAll();
            updateCostSummaryLabels(0.0, 0);
            return;
        }

        LocalDate latestDate = latestRecord.getTimestamp().toLocalDate();
        List<EnergyRecord> latestDayRecords = history.stream()
                .filter(record -> record.getTimestamp().toLocalDate().equals(latestDate))
                .toList();

        Map<Integer, Double> hourlyTotals = latestDayRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getTimestamp().getHour(),
                        LinkedHashMap::new,
                        Collectors.summingDouble(record ->
                                EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh()))
                ));

        List<XYChart.Data<String, Number>> hourlyData = hourlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new XYChart.Data<String, Number>(
                        String.format("%02d:00", entry.getKey()),
                        entry.getValue()))
                .toList();

        costsSeries.getData().setAll(hourlyData);
        hourlyData.forEach(this::installCostTooltipWhenNodeReady);

        double totalCostToday = latestDayRecords.stream()
                .mapToDouble(record ->
                        EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh()))
                .sum();

        updateCostSummaryLabels(totalCostToday, latestDayRecords.size());
    }

    private void installCostTooltipWhenNodeReady(XYChart.Data<String, Number> dataPoint) {
        Platform.runLater(() -> {
            Node node = dataPoint.getNode();
            if (node == null) return;

            Tooltip tip = new Tooltip(
                    dataPoint.getXValue() + "\n" + formatCurrency(dataPoint.getYValue().doubleValue())
            );
            Tooltip.install(node, tip);
        });
    }

    private void updateCostSummaryLabels(double totalCostToday, int todayRecordCount) {
        totalCostTodayLabel.setText(formatCurrency(totalCostToday));

        double elapsedHours = todayRecordCount * 0.5;
        double projectedDailyCost = elapsedHours <= 0
                ? 0.0
                : (totalCostToday / elapsedHours) * 24.0;

        projectedDailyCostLabel.setText(formatCurrency(projectedDailyCost));
    }

    private String formatCurrency(double amount) {
        return String.format("£%.2f", amount);
    }

    private void updateAxisBounds() {
        if (latestEpochSec == 0) return;

        long ub = latestEpochSec;
        long lb;

        if (windowSeconds == -1) {
            lb = getEarliestEpoch();
            if (lb == Long.MIN_VALUE) lb = ub;
        } else {
            lb = ub - windowSeconds;
        }

        usageSmallXAxis.setLowerBound(lb);
        usageSmallXAxis.setUpperBound(ub);
        usageLargeXAxis.setLowerBound(lb);
        usageLargeXAxis.setUpperBound(ub);

        if (windowSeconds > 0) {
            trimToWindow(smallSeries, lb);
            trimToWindow(largeSeries, lb);
        }
    }

    private void trimToWindow(XYChart.Series<Number, Number> series, long lb) {
        if (windowSeconds < 0) return;

        while (!series.getData().isEmpty()) {
            long t = series.getData().get(0).getXValue().longValue();

            if (t >= lb) break;
            series.getData().remove(0);
        }
    }

    private long getLatestEpoch() {
        if (largeSeries.getData().isEmpty()) return Long.MIN_VALUE;
        return largeSeries.getData().get(largeSeries.getData().size() - 1).getXValue().longValue();
    }

    private long getEarliestEpoch() {
        if (largeSeries.getData().isEmpty()) return Long.MIN_VALUE;
        return largeSeries.getData().get(0).getXValue().longValue();
    }

    private void showOverlay() {
        graphOverlay.setVisible(true);
        graphOverlay.setManaged(true);
    }

    private void hideOverlay() {
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        loadEnergySourceBreakdown();
        service.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(this::loadEnergySourceBreakdown);
        });

        if (this.simulationService != null) {
            initServiceConnection();
            if (solarStatusCardController != null) {
                solarStatusCardController.setSimulationService(service);
            }
        }
    }

    private void initServiceConnection() {
        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        for (EnergyRecord rec : history) {
            plotDataPoint(rec);
        }
        refreshCostsChart();

        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                plotDataPoint(newVal);
                refreshCostsChart();
            }
        });
    }

    private void plotDataPoint(EnergyRecord rec) {
        long x = DateTimeUtils.toEpochSeconds(rec.getTimestamp());
        double y = rec.getTotalLoadKwh();

        latestEpochSec = x;

        XYChart.Data<Number, Number> dataSmall = new XYChart.Data<>(x, y);
        XYChart.Data<Number, Number> dataLarge = new XYChart.Data<>(x, y);

        smallSeries.getData().add(dataSmall);
        largeSeries.getData().add(dataLarge);

        installTooltipWhenNodeReady(dataSmall, rec);
        installTooltipWhenNodeReady(dataLarge, rec);

        updateAxisBounds();
        updateDeviceBreakdown(rec);
    }

    public void updateDeviceBreakdown(EnergyRecord currentRec) {
        if (simulationService == null) return;
        Map<String, Double> weeklyTotals = simulationService.getWeeklyAccumulatedUsage();
        allDevices = weeklyTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> new DeviceData(
                        entry.getKey(),
                        String.format("%.2f kWh this week", entry.getValue()),
                        String.format("Weekly Cost: £%.2f", entry.getValue() * 0.28)
                ))
                .collect(Collectors.toList());

        Platform.runLater(this::renderPage);
    }

    private void renderPage() {
        if (allDevices.isEmpty()) return;

        int totalPages = (int) Math.ceil(allDevices.size() / 3.0);

        page1.setVisible(totalPages >= 1); page1.setManaged(totalPages >= 1);
        page2.setVisible(totalPages >= 2); page2.setManaged(totalPages >= 2);
        page3.setVisible(totalPages >= 3); page3.setManaged(totalPages >= 3);

        String active   = "-fx-padding: 4 10; -fx-font-size: 13; -fx-font-weight: bold; -fx-underline: true; -fx-opacity: 1.0;";
        String inactive = "-fx-padding: 4 10; -fx-font-size: 13; -fx-font-weight: normal; -fx-underline: false; -fx-opacity: 0.45;";

        page1.setStyle(currentPage == 0 ? active : inactive);
        page2.setStyle(currentPage == 1 ? active : inactive);
        page3.setStyle(currentPage == 2 ? active : inactive);

        int startIndex = currentPage * 3;
        updateSlot(device1, device1holder, device1useage, weeklycost1, startIndex);
        updateSlot(device2, device2holder, device2useage, weeklycost2, startIndex + 1);
        updateSlot(device3, device3holder, device3useage, weeklycost3, startIndex + 2);
    }

    private void updateSlot(VBox container, Label holder, Label usage, Label cost, int index) {
        if (index < allDevices.size()) {
            DeviceData data = allDevices.get(index);
            holder.setText(data.name);
            usage.setText(data.usage);
            cost.setText(data.weeklyCost);

            container.setVisible(true);
            container.setManaged(true);
        } else {
            container.setVisible(false);
            container.setManaged(false);
        }
    }

    private static class DeviceData {
        String name, usage, weeklyCost;
        DeviceData(String name, String usage, String weeklyCost) {
            this.name = name;
            this.usage = usage;
            this.weeklyCost = weeklyCost;
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

        EnergyRecord current = simulationService.getCurrentEnergyRecord().get();

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
        gridSourcesChart.requestLayout();
        gridSourcesChart.layout();
        System.out.println("DEBUG: Grid chart visually updated at step " + step);
    }
}
