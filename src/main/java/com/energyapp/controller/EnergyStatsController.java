package com.energyapp.controller;

import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import com.energyapp.utils.DateTimeUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import java.util.Map;
import java.util.ArrayList;

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

    @FXML private SolarStatusCardController solarStatusCardController;

    @FXML private VBox device1; // The card for the 1st device
    @FXML private VBox device2; // The card for the 2nd device
    @FXML private VBox device3; // The card for the 3rd device

    @FXML private Label device1holder, device2holder, device3holder;
    @FXML private Label device1useage, device2useage, device3useage;
    @FXML private Label weeklycost1, weeklycost2, weeklycost3;

    private SimulationService simulationService;

    private final XYChart.Series<Number, Number> smallSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> largeSeries = new XYChart.Series<>();

    // window: last hour default, -1 means "all time"
    private long windowSeconds = 60 * 60;
    private long latestEpochSec = 0;

    private enum ViewMode { REAL_TIME, LAST_1_HOUR, LAST_24_HOURS, ALL_TIME }
    private ViewMode viewMode = ViewMode.REAL_TIME;

    @FXML
    public void initialize() {
        // overlay toggling
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
        usageChartSmall.setOnMouseClicked(e -> showOverlay());
        backButton.setOnAction(e -> hideOverlay());

        // series
        usageChartSmall.getData().add(smallSeries);
        usageChartLarge.getData().add(largeSeries);

        // axis formatting
        setupTimeAxis(usageSmallXAxis);
        setupTimeAxis(usageLargeXAxis);

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

            XYChart.Data<Number, Number> ds = new XYChart.Data<>(t, rec.getWatts());
            XYChart.Data<Number, Number> dl = new XYChart.Data<>(t, rec.getWatts());

            smallSeries.getData().add(ds);
            largeSeries.getData().add(dl);

            installTooltipWhenNodeReady(ds, rec);
            installTooltipWhenNodeReady(dl, rec);
        }
    }

    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
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
            double costPounds = EnergyCalculator.calculateCostInPounds(rec.getTotalLoadKwh(), rec.getPricePerKwh());

            double totalLoad = rec.getTotalLoadKwh();
            double solarUsed = EnergyCalculator.calculateSolarUsed(totalLoad, rec.getSolarGenKwh());
            double batteryUsed = EnergyCalculator.calculateBatteryUsed(totalLoad, rec.getSolarGenKwh(), rec.getGridImport());

            int solarPct = EnergyCalculator.calculatePercentage(solarUsed, totalLoad);
            int batteryPct = EnergyCalculator.calculatePercentage(batteryUsed, totalLoad);

            // Simple CO2 label (tweak thresholds if you want)
            String co2Label = EnergyCalculator.determineCo2Label(rec.getCarbon());

            // Tooltip text (matches the style in your screenshot)
            String tipText =
                    time + "\n" +
                            String.format("%.2f kWh • £%.2f • %d%%", rec.getTotalLoadKwh(), costPounds, solarPct) + "\n" +
                            String.format("Price: %.2f p/kWh • Total load: %.0f W", rec.getPricePerKwh(), rec.getWatts()) + "\n" +
                            String.format("Solar, %d%% Battery • %s", batteryPct, co2Label);

            Tooltip tip = new Tooltip(tipText);
            Tooltip.install(node, tip);

            // Click point -> open overlay (nice UX)
            node.setOnMouseClicked(ev -> showOverlay());
        });
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

        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                plotDataPoint(newVal);
            }
        });
    }

    private void plotDataPoint(EnergyRecord rec) {
        long x = DateTimeUtils.toEpochSeconds(rec.getTimestamp());
        double y = rec.getWatts();
        y = y/1000;

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

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(weeklyTotals.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (sorted.size() >= 3) {
            device1holder.setText(sorted.get(0).getKey());
            device1useage.setText(String.format("%.2f kWh this week", sorted.get(0).getValue()));
            double cost1 = sorted.get(0).getValue() * 0.28;
            weeklycost1.setText(String.format("Weekly Cost: £%.2f", cost1));

            device2holder.setText(sorted.get(1).getKey());
            device2useage.setText(String.format("%.2f kWh this week", sorted.get(1).getValue()));
            double cost2 = sorted.get(1).getValue() * 0.28;
            weeklycost2.setText(String.format("Weekly Cost: £%.2f", cost2));


            device3holder.setText(sorted.get(2).getKey());
            device3useage.setText(String.format("%.2f kWh this week", sorted.get(2).getValue()));
            double cost3 = sorted.get(2).getValue() * 0.28;
            weeklycost3.setText(String.format("Weekly Cost: £%.2f", cost3));
        }
    }
}
