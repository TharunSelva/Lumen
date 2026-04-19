package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Geocode;
import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import com.energyapp.service.OpenMeteoSolarService;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.List;

public class ForecastingController {

    @FXML private VBox insightsContainer;
    @FXML private Button forecastingReload;
    @FXML private XYChart<Number, Number> solarForecastChart;

    private SimulationService simulationService;
    private int lastRefreshRecordCount = 0;
    private static final int REFRESH_THRESHOLD = 5;

    private final HomeProfileRepository homeRepo = new HomeProfileRepository();
    private final OpenMeteoSolarService solarService = new OpenMeteoSolarService();

    @FXML
    public void initialize() {
        refreshInsights();
        loadSolarForecast();

        if (forecastingReload != null) {
            forecastingReload.setOnAction(event -> refreshInsights());
        }

        forecastingReload.setOnAction(event -> loadSolarForecast());
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        refreshInsights();
        lastRefreshRecordCount = service.getEnergyStatsSessionHistory().size();
        service.getCurrentEnergyRecord().addListener((obs, oldRec, newRec) -> {
            if (newRec != null) {
                int currentSize = service.getEnergyStatsSessionHistory().size();
                if (currentSize - lastRefreshRecordCount >= REFRESH_THRESHOLD) {
                    javafx.application.Platform.runLater(() -> {
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
        System.out.println("Previous total: " + previousUsageTotal + " kWh, Current total: " + currentUsageTotal + " kWh");
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
        Geocode loc = homeRepo.getHomeLocation("HOME_1");
        if (loc == null) return;

        List<OpenMeteoSolarService.SolarPoint> forecast = null;
        try {
            forecast = solarService.fetchHourlyShortwave(loc.latitude(), loc.longitude(), 2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (forecast == null) return;

        List<OpenMeteoSolarService.SolarPoint> finalForecast = forecast;
        Platform.runLater(() -> {
            solarForecastChart.getData().clear();

            // Lock the X axis to exactly your data range
            javafx.scene.chart.NumberAxis xAxis =
                    (javafx.scene.chart.NumberAxis) solarForecastChart.getXAxis();
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(47);
            xAxis.setTickUnit(1); // one tick per hour

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Solar Generation (W/m²)");

            for (int i = 0; i < finalForecast.size(); i++) {
                OpenMeteoSolarService.SolarPoint point = finalForecast.get(i);
                int hour = Integer.parseInt(point.timeIso.substring(11, 13));
                boolean isDay2 = i >= 24;
                series.getData().add(new XYChart.Data<>(isDay2 ? hour + 24 : hour, point.shortwaveWm2));
            }

            solarForecastChart.getData().add(series);
        });
    }
}