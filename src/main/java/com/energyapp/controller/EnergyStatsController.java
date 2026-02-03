package com.energyapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private final XYChart.Series<Number, Number> smallSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> largeSeries = new XYChart.Series<>();

    // window: last hour default, -1 means "all time"
    private long windowSeconds = 60 * 60;

    private enum ViewMode { REAL_TIME, LAST_1_HOUR, LAST_24_HOURS, ALL_TIME }
    private ViewMode viewMode = ViewMode.REAL_TIME;

    // When user switches to a static mode, we freeze the end time here
    private long frozenEndEpochSec = Long.MIN_VALUE;


    private final List<Row> rows = new ArrayList<>();
    private int rowIndex = 0;

    private Timeline timeline;

    private static final DateTimeFormatter CSV_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static class Row {
        final long epochSec;
        final double watts;
        final double pricePPerKwh;
        final double carbon;

        Row(long epochSec, double watts, double pricePPerKwh, double carbon) {
            this.epochSec = epochSec;
            this.watts = watts;
            this.pricePPerKwh = pricePPerKwh;
            this.carbon = carbon;
        }
    }

    @FXML
    public void initialise() {
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
        timeRangeCombo.setOnAction(e -> {
            String sel = timeRangeCombo.getSelectionModel().getSelectedItem();
            if ("Last 60 mins".equals(sel)) windowSeconds = 60 * 60;
            else if ("Last 24 hours".equals(sel)) windowSeconds = 24 * 60 * 60;
            else windowSeconds = -1;
            refreshAxisWindow();
        });
        timeRangeCombo.setOnAction(e -> onTimeRangeChanged());

        // load + start streaming
        loadCsvFromResources("/com/simulation_data.csv");
        startLiveUpdates();
    }

    // Handler for switching between the dropdown menu options
    private void onTimeRangeChanged() {
        String sel = timeRangeCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        switch (sel) {
            case "Real time" -> {
                viewMode = ViewMode.REAL_TIME;
                // resume live updates
                if (timeline != null) timeline.play();
                // Real time shows last hour window (matches user story)
                windowSeconds = 60 * 60;
            }
            case "Last 1 hour" -> {
                viewMode = ViewMode.LAST_1_HOUR;
                freezeNow();
                windowSeconds = 60 * 60;
            }
            case "Last 24 hours" -> {
                viewMode = ViewMode.LAST_24_HOURS;
                freezeNow();
                windowSeconds = 24 * 60 * 60;
            }
            case "All time" -> {
                viewMode = ViewMode.ALL_TIME;
                freezeNow();
                windowSeconds = -1; // special: all time
            }
        }

        // Apply the selected view to the charts
        applyViewToCharts();
    }

    private void freezeNow() {
        // stop live movement
        if (timeline != null) timeline.pause();

        // freeze at the latest point currently shown; fallback to latest CSV row if needed
        long latest = getLatestEpoch();
        if (latest != Long.MIN_VALUE) {
            frozenEndEpochSec = latest;
        } else if (!rows.isEmpty()) {
            frozenEndEpochSec = rows.get(rows.size() - 1).epochSec;
        }
    }

    private void applyViewToCharts() {
        if (rows.isEmpty()) return;

        long end = (viewMode == ViewMode.REAL_TIME) ? getLatestEpoch() : frozenEndEpochSec;
        if (end == Long.MIN_VALUE) end = rows.get(0).epochSec;

        long start;
        if (viewMode == ViewMode.ALL_TIME) {
            start = rows.get(0).epochSec;
        } else {
            start = end - windowSeconds;
        }

        // IMPORTANT: rebuild series for static views so it stops “sliding”
        if (viewMode != ViewMode.REAL_TIME) {
            rebuildSeriesForRange(start, end);
        }

        // Set fixed axis bounds (static) OR moving bounds (real time)
        usageSmallXAxis.setLowerBound(start);
        usageSmallXAxis.setUpperBound(end);

        usageLargeXAxis.setLowerBound(start);
        usageLargeXAxis.setUpperBound(end);
    }

    private void rebuildSeriesForRange(long startEpochSec, long endEpochSec) {
        smallSeries.getData().clear();
        largeSeries.getData().clear();

        for (Row r : rows) {
            if (r.epochSec < startEpochSec) continue;
            if (r.epochSec > endEpochSec) break;

            smallSeries.getData().add(new XYChart.Data<>(r.epochSec, r.watts));
            largeSeries.getData().add(new XYChart.Data<>(r.epochSec, r.watts));
        }
    }



    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setForceZeroInRange(false);
        axis.setTickUnit(10 * 60);

        axis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number object) {
                long sec = object.longValue();
                LocalTime t = Instant.ofEpochSecond(sec)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                return t.toString().substring(0, 5);
            }
            @Override public Number fromString(String string) { return 0; }
        });
    }

    private void loadCsvFromResources(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("CSV not found on classpath: " + resourcePath);
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            br.readLine(); // header

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);

                LocalDateTime ldt = LocalDateTime.parse(parts[0].trim(), CSV_TS);
                long epochSec = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();

                double priceP = Double.parseDouble(parts[2].trim());
                double carbon = Double.parseDouble(parts[3].trim());
                double totalLoadKwh = Double.parseDouble(parts[4].trim());

                // kWh per 30 mins -> watts
                double watts = totalLoadKwh * 2000.0;

                rows.add(new Row(epochSec, watts, priceP, carbon));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startLiveUpdates() {
        if (rows.isEmpty()) return;

        rowIndex = 0;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> addNextPoint()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void addNextPoint() {
        Row r = rows.get(rowIndex);
        rowIndex = (rowIndex + 1) % rows.size();

        XYChart.Data<Number, Number> dSmall = new XYChart.Data<>(r.epochSec, r.watts);
        XYChart.Data<Number, Number> dLarge = new XYChart.Data<>(r.epochSec, r.watts);

        smallSeries.getData().add(dSmall);
        largeSeries.getData().add(dLarge);

        installTooltipWhenNodeReady(dSmall, r);
        installTooltipWhenNodeReady(dLarge, r);

        if (viewMode == ViewMode.REAL_TIME) {
            refreshAxisWindow();
            trimToWindow(smallSeries);
            trimToWindow(largeSeries);
        }

    }

    private void installTooltipWhenNodeReady(XYChart.Data<Number, Number> d, Row r) {
        Platform.runLater(() -> {
            Node node = d.getNode();
            if (node == null) return;

            String time = Instant.ofEpochSecond(r.epochSec)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime().toString().substring(0, 5);

            Tooltip tip = new Tooltip(
                    time + "\n" +
                            String.format("%.0f W", r.watts) + "\n" +
                            String.format("Price: %.2f p/kWh", r.pricePPerKwh) + "\n" +
                            String.format("CO₂: %.0f g/kWh", r.carbon)
            );
            Tooltip.install(node, tip);

            // clicking a point also opens overlay
            node.setOnMouseClicked(ev -> showOverlay());
        });
    }

    private void refreshAxisWindow() {
        long now = getLatestEpoch();
        if (now == Long.MIN_VALUE) return;

        if (windowSeconds < 0) {
            // all time
            long earliest = getEarliestEpoch();
            if (earliest != Long.MIN_VALUE) {
                usageSmallXAxis.setLowerBound(Math.max(now - 3600, earliest));
                usageSmallXAxis.setUpperBound(now);

                usageLargeXAxis.setLowerBound(earliest);
                usageLargeXAxis.setUpperBound(now);
            }
            return;
        }

        long lower = now - windowSeconds;

        usageSmallXAxis.setLowerBound(lower);
        usageSmallXAxis.setUpperBound(now);

        usageLargeXAxis.setLowerBound(lower);
        usageLargeXAxis.setUpperBound(now);
    }

    private void trimToWindow(XYChart.Series<Number, Number> series) {
        if (windowSeconds < 0) return;

        long now = getLatestEpoch();
        if (now == Long.MIN_VALUE) return;
        long lower = now - windowSeconds;

        while (!series.getData().isEmpty()) {
            long t = series.getData().get(0).getXValue().longValue();
            if (t >= lower) break;
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
}
