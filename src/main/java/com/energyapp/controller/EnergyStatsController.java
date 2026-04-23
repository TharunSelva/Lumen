package com.energyapp.controller;

import com.energyapp.db.HomeEnergyAssetsRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.DateTimeUtils;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnergyStatsController {

    private static final String HOME_ID = "HOME_1";
    private static final double DEFAULT_SOLAR_CAPACITY_KW = 4.0;
    private static final String EMPTY_BATTERY_LABEL = "No battery configured";
    private static final int TOOLTIP_SHOW_DELAY_MS = 80;
    private static final int DENSE_SYMBOL_THRESHOLD = 120;

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

    @FXML private Label summaryTotalEnergyLabel;
    @FXML private Label summarySolarTodayLabel;
    @FXML private Label summaryTotalCostLabel;
    @FXML private Label summaryEstimatedContributionLabel;
    @FXML private Label solarNowValueLabel;
    @FXML private Label solarNowDetailLabel;
    @FXML private Label batteryChargeValueLabel;
    @FXML private Label batteryChargeDetailLabel;
    @FXML private Label sourceContributionSummaryLabel;
    @FXML private Label sourceContributionEstimateLabel;
    @FXML private ColumnConstraints solarContributionColumn;
    @FXML private ColumnConstraints batteryContributionColumn;
    @FXML private ColumnConstraints gridContributionColumn;

    @FXML private SolarStatusCardController solarStatusCardController;

    @FXML private VBox device1;
    @FXML private VBox device2;
    @FXML private VBox device3;

    @FXML private Label device1holder;
    @FXML private Label device2holder;
    @FXML private Label device3holder;
    @FXML private Label device1useage;
    @FXML private Label device2useage;
    @FXML private Label device3useage;
    @FXML private Label weeklycost1;
    @FXML private Label weeklycost2;
    @FXML private Label weeklycost3;
    @FXML private Label page1;
    @FXML private Label page2;
    @FXML private Label page3;

    private final XYChart.Series<Number, Number> smallSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> largeSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> costsSeries = new XYChart.Series<>();
    private final HomeEnergyAssetsRepository homeEnergyAssetsRepository = new HomeEnergyAssetsRepository();

    private final List<DeviceData> allDevices = new ArrayList<>();

    private SimulationService simulationService;
    private ChangeListener<EnergyRecord> currentRecordListener;
    private HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets =
            HomeEnergyAssetsRepository.HomeEnergyAssets.defaults();
    private int currentPage = 0;
    private long latestEpochSec = 0;
    private long latestPlottedEpochSec = Long.MIN_VALUE;
    private UsageRange selectedUsageRange = UsageRange.LAST_8_HOURS;
    private List<EnergyRecord> visibleUsageRecords = List.of();
    private final Tooltip nearestUsageTooltip = new Tooltip();

    enum UsageRange {
        REAL_TIME_1_HOUR("Real time / 1 hour", 60L * 60L),
        LAST_8_HOURS("Last 8 hours", 8L * 60L * 60L),
        LAST_24_HOURS("Last 24 hours", 24L * 60L * 60L),
        LAST_WEEK("Last week", 7L * 24L * 60L * 60L),
        LAST_MONTH("Last month", 30L * 24L * 60L * 60L),
        LAST_3_MONTHS("Last 3 months", 90L * 24L * 60L * 60L),
        ALL_TIME("All time", -1L);

        private final String label;
        private final long windowSeconds;

        UsageRange(String label, long windowSeconds) {
            this.label = label;
            this.windowSeconds = windowSeconds;
        }

        String label() {
            return label;
        }

        long windowSeconds() {
            return windowSeconds;
        }

        static UsageRange fromLabel(String label) {
            for (UsageRange range : values()) {
                if (range.label.equals(label)) {
                    return range;
                }
            }
            return LAST_8_HOURS;
        }
    }

    @FXML
    public void initialize() {
        page1.setOnMouseClicked(e -> {
            currentPage = 0;
            renderPage();
        });
        page2.setOnMouseClicked(e -> {
            currentPage = 1;
            renderPage();
        });
        page3.setOnMouseClicked(e -> {
            currentPage = 2;
            renderPage();
        });

        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
        usageChartSmall.setOnMouseClicked(e -> showOverlay());
        backButton.setOnAction(e -> hideOverlay());

        usageChartSmall.getData().add(smallSeries);
        usageChartLarge.getData().add(largeSeries);
        costsChart.getData().add(costsSeries);
        costsChart.setLegendVisible(false);
        costsXAxis.setLabel("");
        costsYAxis.setLabel("");

        setupTimeAxis(usageSmallXAxis);
        usageSmallXAxis.setLabel("Time");
        usageSmallYAxis.setLabel("Power (kWh)");
        usageSmallYAxis.setAutoRanging(true);

        setupTimeAxis(usageLargeXAxis);
        usageLargeXAxis.setLabel("Time");
        usageLargeYAxis.setLabel("Power (kWh)");
        usageLargeYAxis.setAutoRanging(true);

        timeRangeCombo.getItems().setAll(List.of(UsageRange.values()).stream()
                .map(UsageRange::label)
                .toList());
        timeRangeCombo.getSelectionModel().select(selectedUsageRange.label());
        timeRangeCombo.setOnAction(e -> onTimeRangeChanged());
        installNearestPointHover(usageChartSmall, usageSmallXAxis);
        installNearestPointHover(usageChartLarge, usageLargeXAxis);

        refreshSummaryCard();
        refreshCostsChart();
    }

    public void setSimulationService(SimulationService service) {
        if (this.simulationService != null && currentRecordListener != null) {
            this.simulationService.getCurrentEnergyRecord().removeListener(currentRecordListener);
        }

        this.simulationService = service;
        this.homeEnergyAssets = homeEnergyAssetsRepository.fetchByHomeId(HOME_ID);

        if (solarStatusCardController != null) {
            solarStatusCardController.setHomeEnergyAssets(homeEnergyAssets);
            solarStatusCardController.setSimulationService(service);
        }

        if (this.simulationService != null) {
            initServiceConnection();
        } else {
            rebuildUsageCharts(List.of());
            refreshSummaryCard();
            refreshCostsChart();
        }
    }

    private void initServiceConnection() {
        List<EnergyRecord> history = getAvailableHistory();
        rebuildUsageCharts(history);
        refreshSummaryCard();
        refreshCostsChart();

        if (!history.isEmpty()) {
            updateDeviceBreakdown(history.get(history.size() - 1));
        }

        currentRecordListener = (obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleIncomingRecord(newVal);
            }
        };
        simulationService.getCurrentEnergyRecord().addListener(currentRecordListener);
    }

    private void handleIncomingRecord(EnergyRecord record) {
        Runnable refreshAction = () -> {
            appendUsagePointIfNew(record);
            refreshSummaryCard();
            refreshCostsChart();
            updateDeviceBreakdown(record);
        };

        if (Platform.isFxApplicationThread()) {
            refreshAction.run();
        } else {
            Platform.runLater(refreshAction);
        }
    }

    private void onTimeRangeChanged() {
        String selection = timeRangeCombo.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }

        selectedUsageRange = UsageRange.fromLabel(selection);
        applyViewToCharts();
    }

    private void applyViewToCharts() {
        List<EnergyRecord> history = getAvailableHistory();
        if (history.isEmpty()) {
            rebuildUsageCharts(history);
            return;
        }

        AxisWindow axisWindow = calculateAxisWindow(history, selectedUsageRange);
        long start = axisWindow.startEpochSec();
        long end = axisWindow.endEpochSec();

        latestEpochSec = end;
        rebuildSeriesForRange(start, end, history);
        applyAxisWindow(usageSmallXAxis, axisWindow);
        applyAxisWindow(usageLargeXAxis, axisWindow);
    }

    private void rebuildUsageCharts(List<EnergyRecord> history) {
        smallSeries.getData().clear();
        largeSeries.getData().clear();
        latestEpochSec = 0;
        latestPlottedEpochSec = Long.MIN_VALUE;

        if (history.isEmpty()) {
            usageSmallXAxis.setLowerBound(0);
            usageSmallXAxis.setUpperBound(0);
            usageLargeXAxis.setLowerBound(0);
            usageLargeXAxis.setUpperBound(0);
            return;
        }

        latestPlottedEpochSec = DateTimeUtils.toEpochSeconds(history.get(history.size() - 1).getTimestamp());
        applyViewToCharts();
    }

    private void rebuildSeriesForRange(long startEpochSec, long endEpochSec, List<EnergyRecord> history) {
        smallSeries.getData().clear();
        largeSeries.getData().clear();
        List<EnergyRecord> visibleRecords = new ArrayList<>();

        for (EnergyRecord record : history) {
            long timestamp = DateTimeUtils.toEpochSeconds(record.getTimestamp());
            if (timestamp < startEpochSec) {
                continue;
            }
            if (timestamp > endEpochSec) {
                break;
            }

            XYChart.Data<Number, Number> smallData = new XYChart.Data<>(timestamp, record.getTotalLoadKwh());
            XYChart.Data<Number, Number> largeData = new XYChart.Data<>(timestamp, record.getTotalLoadKwh());

            smallSeries.getData().add(smallData);
            largeSeries.getData().add(largeData);
            visibleRecords.add(record);

            installTooltipWhenNodeReady(smallData, record);
            installTooltipWhenNodeReady(largeData, record);
        }

        visibleUsageRecords = visibleRecords;
        boolean denseRange = visibleRecords.size() > DENSE_SYMBOL_THRESHOLD;
        usageChartSmall.setCreateSymbols(!denseRange);
        usageChartLarge.setCreateSymbols(!denseRange);
    }

    private void appendUsagePointIfNew(EnergyRecord record) {
        long timestamp = DateTimeUtils.toEpochSeconds(record.getTimestamp());
        if (timestamp <= latestPlottedEpochSec) {
            return;
        }

        latestPlottedEpochSec = timestamp;
        latestEpochSec = timestamp;

        XYChart.Data<Number, Number> smallData = new XYChart.Data<>(timestamp, record.getTotalLoadKwh());
        XYChart.Data<Number, Number> largeData = new XYChart.Data<>(timestamp, record.getTotalLoadKwh());

        smallSeries.getData().add(smallData);
        largeSeries.getData().add(largeData);

        installTooltipWhenNodeReady(smallData, record);
        installTooltipWhenNodeReady(largeData, record);
        updateAxisBounds();
    }

    private void refreshSummaryCard() {
        SummarySnapshot summary = buildSummarySnapshot();

        summaryTotalEnergyLabel.setText(String.format("%.1f kWh Total Energy", summary.totalEnergyToday()));
        summarySolarTodayLabel.setText(String.format("%.1f kWh Solar Today", summary.totalSolarToday()));
        summaryTotalCostLabel.setText(String.format("%s Total Cost", formatCurrency(summary.totalCostToday())));
        summaryEstimatedContributionLabel.setText(String.format(
                "%d%% solar \u2022 %d%% battery \u2022 %d%% grid",
                summary.sourceBreakdown().solarPercentDisplay(),
                summary.sourceBreakdown().batteryPercentDisplay(),
                summary.sourceBreakdown().gridPercentDisplay()
        ));

        updateEnergySourcesCard(summary);
    }

    private List<EnergyRecord> getLatestDayRecords() {
        return getLatestDayRecords(getAvailableHistory());
    }

    private List<EnergyRecord> getLatestDayRecords(List<EnergyRecord> history) {
        return getLatestDayRecordsForHistory(history);
    }

    static List<EnergyRecord> getLatestDayRecordsForHistory(List<EnergyRecord> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        EnergyRecord latestRecord = history.get(history.size() - 1);
        LocalDate latestDay = latestRecord.getTimestamp().toLocalDate();

        return history.stream()
                .filter(record -> record.getTimestamp().toLocalDate().equals(latestDay))
                .toList();
    }

    static SourceBreakdown calculateSourceBreakdown(List<EnergyRecord> latestDayRecords) {
        double totalLoad = 0.0;
        double solarUsedTotal = 0.0;
        double batteryUsedTotal = 0.0;
        double gridUsedTotal = 0.0;

        for (EnergyRecord record : latestDayRecords) {
            double totalLoadForRecord = Math.max(record.getTotalLoadKwh(), 0.0);
            double solarUsed = Math.min(totalLoadForRecord, Math.max(record.getSolarGenKwh(), 0.0));
            double remainingAfterSolar = Math.max(totalLoadForRecord - solarUsed, 0.0);
            double gridUsed = Math.min(Math.max(record.getGridImport(), 0.0), remainingAfterSolar);
            double batteryUsed = Math.max(totalLoadForRecord - solarUsed - gridUsed, 0.0);

            totalLoad += totalLoadForRecord;
            solarUsedTotal += solarUsed;
            batteryUsedTotal += batteryUsed;
            gridUsedTotal += gridUsed;
        }

        double solarPercent = toPercent(solarUsedTotal, totalLoad);
        double batteryPercent = toPercent(batteryUsedTotal, totalLoad);
        double gridPercent = toPercent(gridUsedTotal, totalLoad);

        return new SourceBreakdown(
                solarUsedTotal,
                batteryUsedTotal,
                gridUsedTotal,
                solarPercent,
                batteryPercent,
                gridPercent,
                toDisplayPercent(solarPercent),
                toDisplayPercent(batteryPercent),
                toDisplayPercent(gridPercent)
        );
    }

    private void updateEnergySourcesCard(SummarySnapshot summary) {
        EnergyRecord latestRecord = summary.latestRecord();
        SourceBreakdown sourceBreakdown = summary.sourceBreakdown();

        if (latestRecord == null) {
            solarNowValueLabel.setText(homeEnergyAssets.hasSolar() ? "0.0 kW" : "Solar not configured");
            solarNowDetailLabel.setText(homeEnergyAssets.hasSolar()
                    ? String.format("0%% of %.1f kW capacity", homeEnergyAssets.solarCapacityOrDefault(DEFAULT_SOLAR_CAPACITY_KW))
                    : "Using a non-configured fallback state.");
            batteryChargeValueLabel.setText(homeEnergyAssets.hasBattery() ? "0%" : EMPTY_BATTERY_LABEL);
            batteryChargeDetailLabel.setText(homeEnergyAssets.hasBattery() ? "0.0 kWh stored right now" : "Battery details unavailable");
            sourceContributionSummaryLabel.setText("0% solar \u2022 0% battery \u2022 0% grid");
            sourceContributionEstimateLabel.setText("Estimated source contribution");
            updateContributionBar(0.0, 0.0, 0.0);
            return;
        }

        double solarCapacityKw = homeEnergyAssets.solarCapacityOrDefault(DEFAULT_SOLAR_CAPACITY_KW);
        double currentSolarKw = Math.max(latestRecord.getSolarGenKwh() * 2.0, 0.0);
        double solarCapacityPercent = solarCapacityKw <= 0.0
                ? 0.0
                : clamp((currentSolarKw / solarCapacityKw) * 100.0, 0.0, 100.0);

        if (homeEnergyAssets.hasSolar()) {
            solarNowValueLabel.setText(String.format("%.1f kW", currentSolarKw));
            solarNowDetailLabel.setText(String.format("%.0f%% of capacity", solarCapacityPercent));
        } else {
            solarNowValueLabel.setText("Solar not configured");
            solarNowDetailLabel.setText("Using a non-configured fallback state.");
        }

        if (homeEnergyAssets.hasBattery() && homeEnergyAssets.batteryCapacityKwh() != null) {
            double storedBatteryKwh = homeEnergyAssets.batteryCapacityKwh() * (latestRecord.getBatteryLevel() / 100.0);
            batteryChargeValueLabel.setText(String.format("%d%%", latestRecord.getBatteryLevel()));
            batteryChargeDetailLabel.setText(String.format("%.1f / %.1f kWh", storedBatteryKwh, homeEnergyAssets.batteryCapacityKwh()));
        } else if (homeEnergyAssets.hasBattery()) {
            batteryChargeValueLabel.setText(String.format("%d%%", latestRecord.getBatteryLevel()));
            batteryChargeDetailLabel.setText("Battery capacity unavailable");
        } else {
            batteryChargeValueLabel.setText(EMPTY_BATTERY_LABEL);
            batteryChargeDetailLabel.setText("Battery details unavailable");
        }

        sourceContributionSummaryLabel.setText(String.format(
                "%d%% solar \u2022 %d%% battery \u2022 %d%% grid",
                sourceBreakdown.solarPercentDisplay(),
                sourceBreakdown.batteryPercentDisplay(),
                sourceBreakdown.gridPercentDisplay()
        ));
        sourceContributionEstimateLabel.setText("Estimated source contribution");
        updateContributionBar(
                sourceBreakdown.solarPercent(),
                sourceBreakdown.batteryPercent(),
                sourceBreakdown.gridPercent()
        );
    }

    private void updateContributionBar(double solarPercent, double batteryPercent, double gridPercent) {
        solarContributionColumn.setPercentWidth(solarPercent);
        batteryContributionColumn.setPercentWidth(batteryPercent);
        gridContributionColumn.setPercentWidth(gridPercent);
    }

    private SummarySnapshot buildSummarySnapshot() {
        return buildSummarySnapshot(getAvailableHistory(), homeEnergyAssets);
    }

    static SummarySnapshot buildSummarySnapshot(
            List<EnergyRecord> history,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets
    ) {
        List<EnergyRecord> latestDayRecords = getLatestDayRecordsForHistory(history);
        if (latestDayRecords.isEmpty()) {
            return SummarySnapshot.empty(homeEnergyAssets);
        }

        EnergyRecord latestRecord = latestDayRecords.get(latestDayRecords.size() - 1);
        double totalEnergyToday = latestDayRecords.stream()
                .mapToDouble(EnergyRecord::getTotalLoadKwh)
                .sum();
        double totalSolarToday = latestDayRecords.stream()
                .mapToDouble(EnergyRecord::getSolarGenKwh)
                .sum();
        double totalCostToday = latestDayRecords.stream()
                .mapToDouble(record -> EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh()))
                .sum();

        return new SummarySnapshot(
                latestRecord,
                totalEnergyToday,
                totalSolarToday,
                totalCostToday,
                calculateSourceBreakdown(latestDayRecords),
                homeEnergyAssets
        );
    }

    private void refreshCostsChart() {
        List<EnergyRecord> history = getAvailableHistory();
        if (history.isEmpty()) {
            costsSeries.getData().clear();
            updateCostSummaryLabels(0.0, 0);
            return;
        }

        List<EnergyRecord> latestDayRecords = getLatestDayRecords(history);
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
                        entry.getValue()
                ))
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
            if (node == null) {
                return;
            }

            Tooltip tip = new Tooltip(
                    dataPoint.getXValue() + "\n" + formatCurrency(dataPoint.getYValue().doubleValue())
            );
            Tooltip.install(node, tip);
        });
    }

    private void updateCostSummaryLabels(double totalCostToday, int todayRecordCount) {
        totalCostTodayLabel.setText(formatCurrency(totalCostToday));

        double elapsedHours = todayRecordCount * 0.5;
        double projectedDailyCost = elapsedHours <= 0.0
                ? 0.0
                : (totalCostToday / elapsedHours) * 24.0;

        projectedDailyCostLabel.setText(formatCurrency(projectedDailyCost));
    }

    private void updateAxisBounds() {
        if (latestEpochSec == 0) {
            return;
        }

        AxisWindow axisWindow = calculateAxisWindow(getAvailableHistory(), selectedUsageRange);
        applyAxisWindow(usageSmallXAxis, axisWindow);
        applyAxisWindow(usageLargeXAxis, axisWindow);

        if (selectedUsageRange.windowSeconds() > 0) {
            trimToWindow(smallSeries, axisWindow.startEpochSec());
            trimToWindow(largeSeries, axisWindow.startEpochSec());
        }

        visibleUsageRecords = getVisibleRecords(getAvailableHistory(), axisWindow);
    }

    private void trimToWindow(XYChart.Series<Number, Number> series, long lowerBound) {
        if (selectedUsageRange.windowSeconds() < 0) {
            return;
        }

        while (!series.getData().isEmpty()) {
            long timestamp = series.getData().get(0).getXValue().longValue();
            if (timestamp >= lowerBound) {
                break;
            }
            series.getData().remove(0);
        }
    }

    private long getEarliestEpoch() {
        if (largeSeries.getData().isEmpty()) {
            return Long.MIN_VALUE;
        }
        return largeSeries.getData().get(0).getXValue().longValue();
    }

    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setLabel("Time");
        axis.setForceZeroInRange(false);
        applyAxisWindow(axis, new AxisWindow(0, 8L * 60L * 60L, 8L * 60L * 60L));
    }

    private void installTooltipWhenNodeReady(XYChart.Data<Number, Number> dataPoint, EnergyRecord record) {
        Platform.runLater(() -> {
            Node node = dataPoint.getNode();
            if (node == null) {
                return;
            }

            Tooltip tooltip = new Tooltip(formatUsageTooltip(record));
            tooltip.setShowDelay(Duration.millis(TOOLTIP_SHOW_DELAY_MS));
            tooltip.setHideDelay(Duration.millis(40));
            tooltip.setShowDuration(Duration.seconds(12));
            Tooltip.install(node, tooltip);
            node.setOnMouseClicked(ev -> showOverlay());
        });
    }

    private String formatUsageTooltip(EnergyRecord record) {
        long epoch = DateTimeUtils.toEpochSeconds(record.getTimestamp());
        String time = DateTimeUtils.formatTimeFromEpoch(epoch);
        double costPounds = EnergyCalculator.calculateCostInPounds(record.getGridImport(), record.getPricePerKwh());
        double totalLoad = record.getTotalLoadKwh();
        double solarUsed = EnergyCalculator.calculateSolarUsed(totalLoad, record.getSolarGenKwh());
        double batteryUsed = EnergyCalculator.calculateBatteryUsed(totalLoad, record.getSolarGenKwh(), record.getGridImport());

        int solarPct = EnergyCalculator.calculatePercentage(solarUsed, totalLoad);
        int batteryPct = EnergyCalculator.calculatePercentage(batteryUsed, totalLoad);
        String co2Label = EnergyCalculator.determineCo2Label(record.getCarbon());

        return time + "\n" +
                String.format("%.2f kWh \u2022 %s \u2022 %d%%", record.getTotalLoadKwh(), formatCurrency(costPounds), solarPct) + "\n" +
                String.format("Price: %.2f p/kWh \u2022 Total load: %.0f W", record.getPricePerKwh(), record.getWatts()) + "\n" +
                String.format("Solar, %d%% Battery \u2022 %s", batteryPct, co2Label);
    }

    private void installNearestPointHover(LineChart<Number, Number> chart, NumberAxis xAxis) {
        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (visibleUsageRecords.isEmpty()) {
                nearestUsageTooltip.hide();
                return;
            }

            double axisX = xAxis.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
            Number axisValue = xAxis.getValueForDisplay(axisX);
            if (axisValue == null) {
                nearestUsageTooltip.hide();
                return;
            }

            EnergyRecord nearest = findNearestRecord(visibleUsageRecords, axisValue.longValue());
            if (nearest == null) {
                nearestUsageTooltip.hide();
                return;
            }

            nearestUsageTooltip.setText(formatUsageTooltip(nearest));
            if (!nearestUsageTooltip.isShowing()) {
                nearestUsageTooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 12);
            } else {
                nearestUsageTooltip.setX(event.getScreenX() + 14);
                nearestUsageTooltip.setY(event.getScreenY() + 12);
            }
        });
        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> nearestUsageTooltip.hide());
    }

    private EnergyRecord findNearestRecord(List<EnergyRecord> records, long targetEpochSec) {
        EnergyRecord nearest = null;
        long smallestDistance = Long.MAX_VALUE;

        for (EnergyRecord record : records) {
            long distance = Math.abs(DateTimeUtils.toEpochSeconds(record.getTimestamp()) - targetEpochSec);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                nearest = record;
            }
        }

        return nearest;
    }

    public void updateDeviceBreakdown(EnergyRecord currentRecord) {
        if (simulationService == null) {
            return;
        }

        Map<String, Double> weeklyTotals = simulationService.getWeeklyAccumulatedUsage();
        allDevices.clear();
        allDevices.addAll(weeklyTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> new DeviceData(
                        entry.getKey(),
                        String.format("%.2f kWh this week", entry.getValue()),
                        String.format("Weekly Cost: %s", formatCurrency(entry.getValue() * 0.28))
                ))
                .toList());

        Platform.runLater(this::renderPage);
    }

    private void renderPage() {
        int totalPages = (int) Math.ceil(allDevices.size() / 3.0);

        page1.setVisible(totalPages >= 1);
        page1.setManaged(totalPages >= 1);
        page2.setVisible(totalPages >= 2);
        page2.setManaged(totalPages >= 2);
        page3.setVisible(totalPages >= 3);
        page3.setManaged(totalPages >= 3);

        String active = "-fx-padding: 4 10; -fx-font-size: 13; -fx-font-weight: bold; -fx-underline: true; -fx-opacity: 1.0;";
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
            holder.setText(data.name());
            usage.setText(data.usage());
            cost.setText(data.weeklyCost());
            container.setVisible(true);
            container.setManaged(true);
        } else {
            container.setVisible(false);
            container.setManaged(false);
        }
    }

    private List<EnergyRecord> getAvailableHistory() {
        if (simulationService == null) {
            return List.of();
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        if (!history.isEmpty()) {
            return history;
        }

        EnergyRecord currentRecord = simulationService.getCurrentEnergyRecord().get();
        return currentRecord == null ? List.of() : List.of(currentRecord);
    }

    private String formatCurrency(double amount) {
        return String.format("\u00A3%.2f", amount);
    }

    private static double toPercent(double part, double total) {
        if (total <= 0.0) {
            return 0.0;
        }
        return clamp((part / total) * 100.0, 0.0, 100.0);
    }

    private static int toDisplayPercent(double percent) {
        return (int) Math.round(clamp(percent, 0.0, 100.0));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void showOverlay() {
        graphOverlay.setVisible(true);
        graphOverlay.setManaged(true);
    }

    private void hideOverlay() {
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
    }

    static AxisWindow calculateAxisWindow(List<EnergyRecord> history, UsageRange range) {
        if (history == null || history.isEmpty()) {
            return new AxisWindow(0, 0, 0);
        }

        long earliest = DateTimeUtils.toEpochSeconds(history.get(0).getTimestamp());
        long latest = DateTimeUtils.toEpochSeconds(history.get(history.size() - 1).getTimestamp());
        long start = range.windowSeconds() < 0
                ? earliest
                : Math.max(earliest, latest - range.windowSeconds());

        if (start > latest) {
            start = latest;
        }

        return new AxisWindow(start, latest, Math.max(latest - start, 0));
    }

    static long chooseTickUnitSeconds(long visibleDurationSeconds) {
        if (visibleDurationSeconds <= 2L * 60L * 60L) {
            return 10L * 60L;
        }
        if (visibleDurationSeconds <= 8L * 60L * 60L) {
            return 60L * 60L;
        }
        if (visibleDurationSeconds <= 24L * 60L * 60L) {
            return 3L * 60L * 60L;
        }
        if (visibleDurationSeconds <= 7L * 24L * 60L * 60L) {
            return 24L * 60L * 60L;
        }
        if (visibleDurationSeconds <= 31L * 24L * 60L * 60L) {
            return 7L * 24L * 60L * 60L;
        }
        return 14L * 24L * 60L * 60L;
    }

    static String formatAxisTick(long epochSeconds, long visibleDurationSeconds) {
        var dateTime = Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        if (visibleDurationSeconds <= 24L * 60L * 60L) {
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (visibleDurationSeconds <= 7L * 24L * 60L * 60L) {
            return dateTime.format(DateTimeFormatter.ofPattern("EEE HH:mm"));
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    private void applyAxisWindow(NumberAxis axis, AxisWindow axisWindow) {
        long duration = axisWindow.visibleDurationSeconds();
        axis.setLowerBound(axisWindow.startEpochSec());
        axis.setUpperBound(axisWindow.endEpochSec() <= axisWindow.startEpochSec()
                ? axisWindow.startEpochSec() + 1
                : axisWindow.endEpochSec());
        axis.setTickUnit(chooseTickUnitSeconds(duration));
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return formatAxisTick(object.longValue(), duration);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private List<EnergyRecord> getVisibleRecords(List<EnergyRecord> history, AxisWindow axisWindow) {
        if (history.isEmpty()) {
            return List.of();
        }

        return history.stream()
                .filter(record -> {
                    long timestamp = DateTimeUtils.toEpochSeconds(record.getTimestamp());
                    return timestamp >= axisWindow.startEpochSec() && timestamp <= axisWindow.endEpochSec();
                })
                .toList();
    }

    private record DeviceData(String name, String usage, String weeklyCost) { }

    record SummarySnapshot(
            EnergyRecord latestRecord,
            double totalEnergyToday,
            double totalSolarToday,
            double totalCostToday,
            SourceBreakdown sourceBreakdown,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets
    ) {
        private static SummarySnapshot empty(HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets) {
            return new SummarySnapshot(
                    null,
                    0.0,
                    0.0,
                    0.0,
                    SourceBreakdown.empty(),
                    homeEnergyAssets
            );
        }
    }

    record SourceBreakdown(
            double solarKwh,
            double batteryKwh,
            double gridKwh,
            double solarPercent,
            double batteryPercent,
            double gridPercent,
            int solarPercentDisplay,
            int batteryPercentDisplay,
            int gridPercentDisplay
    ) {
        private static SourceBreakdown empty() {
            return new SourceBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0);
        }
    }

    record AxisWindow(long startEpochSec, long endEpochSec, long visibleDurationSeconds) { }
}
