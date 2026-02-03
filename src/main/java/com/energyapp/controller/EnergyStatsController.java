package com.energyapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

    // default last hour
    private long windowSeconds = 60 * 60;

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

        // load + start streaming
        loadCsvFromResources("/com/energyapp/view/simulation.csv");
        startLiveUpdates();
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

        // 1 point per second simulation
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> addNextPoint()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void addNextPoint() {
        Row r = rows.get(rowIndex);
        rowIndex = (rowIndex + 1) % rows.size();

        smallSeries.getData().add(new XYChart.Data<>(r.epochSec, r.watts));
        largeSeries.getData().add(new XYChart.Data<>(r.epochSec, r.watts));

        refreshAxisWindow();
        trimToWindow(smallSeries);
        trimToWindow(largeSeries);
    }

    private void refreshAxisWindow() {
        long now = getLatestEpoch();
        if (now == Long.MIN_VALUE) return;

        long lower = now - windowSeconds;

        usageSmallXAxis.setLowerBound(lower);
        usageSmallXAxis.setUpperBound(now);

        usageLargeXAxis.setLowerBound(lower);
        usageLargeXAxis.setUpperBound(now);
    }

    private void trimToWindow(XYChart.Series<Number, Number> series) {
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

    private void showOverlay() {
        graphOverlay.setVisible(true);
        graphOverlay.setManaged(true);
    }

    private void hideOverlay() {
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);
    }
}