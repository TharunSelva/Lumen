package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ForecastingController implements Initializable {

    @FXML private VBox insightsContainer;
    @FXML private Button forecastingReload;
    @FXML private LineChart<String, Number> solarForecastChart;

    private SimulationService simulationService;
    private int lastRefreshRecordCount = 0;
    private static final int REFRESH_THRESHOLD = 5;

    public ForecastingController() {
        System.out.println("=== FORECASTINGCONTROLLER CONSTRUCTOR CALLED ===");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshInsights();

        if (forecastingReload != null) {
            forecastingReload.setOnAction(event -> refreshInsights());
        }

        loadSolarForecast();
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        refreshInsights();
        lastRefreshRecordCount = service.getEnergyStatsSessionHistory().size();

        service.getCurrentEnergyRecord().addListener((obs, oldRec, newRec) -> {
            if (newRec != null) {
                int currentSize = service.getEnergyStatsSessionHistory().size();
                if (currentSize - lastRefreshRecordCount >= REFRESH_THRESHOLD) {
                    Platform.runLater(() -> {
                        refreshInsights();
                        lastRefreshRecordCount = currentSize;
                    });
                }
            }
        });
    }

    private void refreshInsights() {
        if (simulationService == null) return;
        if (insightsContainer.getChildren().size() > 1) {
            insightsContainer.getChildren().remove(1, insightsContainer.getChildren().size());
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();

        double currentUsageTotal = 0.0;
        double previousUsageTotal = 0.0;

        if (history.size() >= 10) {
            int mid = history.size() - 5;
            for (int i = history.size() - 1; i >= mid; i--) {
                currentUsageTotal += history.get(i).getEvChargerUsageKwh()
                        + history.get(i).getWashingMachineUsageKwh()
                        + history.get(i).getHeatingUsageKwh()
                        + history.get(i).getBaseloadKwh();
            }
            for (int i = mid - 1; i >= mid - 5; i--) {
                previousUsageTotal += history.get(i).getEvChargerUsageKwh()
                        + history.get(i).getWashingMachineUsageKwh()
                        + history.get(i).getHeatingUsageKwh()
                        + history.get(i).getBaseloadKwh();
            }
        }

        InsightEngine engine = new InsightEngine();
        List<Insight> activeInsights = engine.generateInsights(currentUsageTotal, previousUsageTotal);

        for (Insight insight : activeInsights) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/InsightCard.fxml"));
                HBox card = loader.load();
                InsightCardController cardController = loader.getController();
                Color statusColor = insight.getTitle().contains("Alert") ? Color.ORANGE : Color.LIGHTGREEN;
                cardController.setContent(insight.getDescription(), statusColor);
                insightsContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadSolarForecast() {
        System.out.println("DEBUG: loadSolarForecast() called - adding bell curve data");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Forecasted Solar Generation");

        double[] hourlyGeneration = generateSolarBellCurve();
        String[] times = {
                "00:00", "01:00", "02:00", "03:00", "04:00", "05:00",
                "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
        };

        for (int i = 0; i < 24; i++) {
            series.getData().add(new XYChart.Data<>(times[i], hourlyGeneration[i]));
        }

        solarForecastChart.getData().clear();
        solarForecastChart.getData().add(series);

        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: #4CAF50; -fx-stroke-width: 3;");
        }

        solarForecastChart.requestLayout();
        System.out.println("DEBUG: Solar forecast chart updated with " + series.getData().size() + " points");
    }

    private double[] generateSolarBellCurve() {
        double[] data = new double[24];
        double peakHour = 13.0;
        double sigma = 3.5;
        double maxkWh = 4.8;

        for (int hour = 0; hour < 24; hour++) {
            double t = hour;
            double exponent = -Math.pow(t - peakHour, 2) / (2 * sigma * sigma);
            double value = maxkWh * Math.exp(exponent);
            if (hour < 5 || hour > 19) value = 0.0;
            data[hour] = Math.round(value * 100.0) / 100.0;
        }
        return data;
    }
}