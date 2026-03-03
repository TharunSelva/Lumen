package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.List;

public class ForecastingController {

    @FXML private VBox insightsContainer;
    @FXML private Button forecastingReload;

    private SimulationService simulationService;
    private int lastRefreshRecordCount = 0;
    private static final int REFRESH_THRESHOLD = 5;

    @FXML
    public void initialize() {
        refreshInsights();

        if (forecastingReload != null) {
            forecastingReload.setOnAction(event -> refreshInsights());
        }
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
}