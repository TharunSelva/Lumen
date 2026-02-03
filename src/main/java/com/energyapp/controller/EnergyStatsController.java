package com.energyapp;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

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

        // axis formatting: epoch seconds -> HH:mm
        setupTimeAxis(usageSmallXAxis);
        setupTimeAxis(usageLargeXAxis);
    }

    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setForceZeroInRange(false);

        // default tick every 10 mins
        axis.setTickUnit(10 * 60);

        axis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number object) {
                long sec = object.longValue();
                LocalTime t = Instant.ofEpochSecond(sec)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                return t.toString().substring(0, 5); // HH:mm
            }
            @Override public Number fromString(String string) { return 0; }
        });
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
