package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import com.energyapp.service.RecommendationEngine;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class RecommendationsController {

    @FXML
    private VBox recommendationsContainer;

    private SimulationService simulationService;
    private RecommendationEngine recommendationEngine;

    @FXML
    public void initialize() {
        this.recommendationEngine = new RecommendationEngine();
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;

        this.simulationService.getCurrentEnergyRecord().addListener((
                obs, oldRec, newRec) -> {
            updateRecommendationsUI(newRec);
        });
    }

    private void updateRecommendationsUI(EnergyRecord currentRecord) {
        // as in other parts of the app, MUST use runLater as sim service on background thread.
        Platform.runLater(() -> {
            recommendationsContainer.getChildren().clear();
            List<Recommendation> activeRecommendations = recommendationEngine.getActiveRecommendations(currentRecord);

            for (Recommendation recco : activeRecommendations) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/RecommendationCard.fxml"));
                    Node cardNode = loader.load();

                    RecommendationCardController reccoController = loader.getController();
                    reccoController.setRecommendation(recco);

                    recommendationsContainer.getChildren().add(cardNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}


