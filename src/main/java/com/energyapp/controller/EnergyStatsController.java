package com.energyapp;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;

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

    @FXML
    public void initialize() {
        // start hidden
        graphOverlay.setVisible(false);
        graphOverlay.setManaged(false);

        // click small chart -> open overlay
        usageChartSmall.setOnMouseClicked(e -> showOverlay());

        // back button -> close overlay
        backButton.setOnAction(e -> hideOverlay());
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