package com.energyapp.controller;

import com.energyapp.model.ViewType;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.application.Platform;
import java.net.URL;

public class MainLayoutController {

    @FXML private BorderPane rootLayout;
    @FXML private SideBarController mainSideBarController;

    private SimulationService simulationService;

    public void initialize() {
        System.out.println("Main layout controller successfully loaded");

        if (mainSideBarController != null) {
            mainSideBarController.setMainController(this);
        }

    }


        private void switchView (ViewType viewType){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewType.getFxmlPath()));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardController) {
                DashboardController dc = (DashboardController) controller;
                dc.setMainController(this);
                dc.setSimService(this.simulationService);
            } else if (controller instanceof EnergyStatsController) {
                ((EnergyStatsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof RecommendationsController) {
                ((RecommendationsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof EcoStatisticsController) {
                ((EcoStatisticsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof ForecastingController) {
                    ((ForecastingController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof DevicesController) {
                ((DevicesController) controller).setSimulationService(this.simulationService);
            }
            // --- its either FIGMA BALLS OR LIGMA BALLS !!!! ---
            try {
                URL adUrl = getClass().getResource("/com/energyapp/coc-ad.mp4");
                if (adUrl == null) {
                    rootLayout.setCenter(root); // Fallback: load normal page if video missing
                } else {
                    Media media = new Media(adUrl.toString());
                    MediaPlayer adPlayer = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(adPlayer);

                    // Make the video fill the space
                    mediaView.setFitWidth(1000);
                    mediaView.setPreserveRatio(true);

                    // Create the skip button
                    Button skipButton = new Button("Skip Ad in 15s...");
                    skipButton.setDisable(true); // Disable clicking!
                    skipButton.setStyle("-fx-font-size: 18px; -fx-background-color: #ff4444; -fx-text-fill: white; -fx-padding: 10px; -fx-font-weight: bold;");

                    // Stack them together
                    StackPane adPane = new StackPane(mediaView, skipButton);
                    StackPane.setAlignment(skipButton, Pos.TOP_RIGHT); // Put skip button in top right corner
                    StackPane.setMargin(skipButton, new javafx.geometry.Insets(20));

                    rootLayout.setCenter(adPane); // Hijack the screen with the ad!
                    adPlayer.play();

                    // The 15-second countdown timer
                    Thread countdown = new Thread(() -> {
                        for (int i = 15; i > 0; i--) {
                            final int timeLeft = i;
                            Platform.runLater(() -> skipButton.setText("Skip Ad in " + timeLeft + "s..."));
                            try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        }
                        // When it reaches 0, unlock the button
                        Platform.runLater(() -> {
                            skipButton.setText("Skip Ad >>");
                            skipButton.setDisable(false);
                        });
                    });
                    countdown.setDaemon(true);
                    countdown.start();

                    // When they finally click Skip, stop the video and load the real page
                    skipButton.setOnAction(e -> {
                        adPlayer.stop();
                        rootLayout.setCenter(root);
                    });
                }
            } catch (Exception e) {
                System.out.println("Ad failed to load: " + e.getMessage());
                rootLayout.setCenter(root); // Safe fallback
            }
            // --------------------------------

    } catch(IOException e){
        e.printStackTrace();
    }
    }


    @FXML
    public void handleShowDashboard() {
        switchView(ViewType.DASHBOARD);
    }

    @FXML
    public void handleShowDevices() {
        switchView(ViewType.DEVICES);
    }

    @FXML
    public void handleShowStatistics() {
        switchView(ViewType.ENERGYSTATISTICS);
    }

    @FXML
    public void handleShowSettings() {
        switchView(ViewType.SETTINGS);
    }

    public void setSimService (SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public void handleShowForecasting() {
        switchView(ViewType.FORECASTING);
    }

    public void handleShowRecommendations () {
        switchView(ViewType.RECOMMENDATIONS);
    }
    public void handleShowEcoStatistics() {
        switchView(ViewType.ECOIMPACT);
    }
}
