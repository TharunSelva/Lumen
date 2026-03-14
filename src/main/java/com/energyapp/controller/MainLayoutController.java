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
    private int adCount = 0;
    private boolean isPlusSubscriber = false;

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
            // --- UNSKIPPABLE AD INJECTION ---
            try {
                URL adUrl = getClass().getResource("/com/energyapp/coc-ad.mp4"); // OR cocad.mp4 if you kept that name!
                if (adUrl == null) {
                    rootLayout.setCenter(root);
                } else {
                    if (com.energyapp.App.bgMusicPlayer != null) {
                        com.energyapp.App.bgMusicPlayer.setVolume(0.1);
                    }

                    Media media = new Media(adUrl.toString());
                    MediaPlayer adPlayer = new MediaPlayer(media);
                    adPlayer.setOnError(() -> rootLayout.setCenter(root));

                    MediaView mediaView = new MediaView(adPlayer);
                    mediaView.setFitWidth(1000);
                    mediaView.setPreserveRatio(true);

                    // Check if they paid the ransom (5 seconds vs 15 seconds)
                    int waitTime = isPlusSubscriber ? 5 : 15;

                    Button skipButton = new Button("Skip Ad in " + waitTime + "s...");
                    skipButton.setDisable(true);
                    skipButton.setStyle("-fx-font-size: 18px; -fx-background-color: #ff4444; -fx-text-fill: white; -fx-padding: 10px; -fx-font-weight: bold;");

                    javafx.scene.layout.StackPane adPane = new javafx.scene.layout.StackPane(mediaView, skipButton);
                    javafx.scene.layout.StackPane.setAlignment(skipButton, javafx.geometry.Pos.TOP_RIGHT);
                    javafx.scene.layout.StackPane.setMargin(skipButton, new javafx.geometry.Insets(20));

                    rootLayout.setCenter(adPane);
                    adPlayer.play();

                    Thread countdown = new Thread(() -> {
                        for (int i = waitTime; i > 0; i--) {
                            final int timeLeft = i;
                            javafx.application.Platform.runLater(() -> skipButton.setText("Skip Ad in " + timeLeft + "s..."));
                            try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        }
                        javafx.application.Platform.runLater(() -> {
                            skipButton.setText("Skip Ad >>");
                            skipButton.setDisable(false);
                        });
                    });
                    countdown.setDaemon(true);
                    countdown.start();

                    skipButton.setOnAction(e -> {
                        adPlayer.stop();
                        if (com.energyapp.App.bgMusicPlayer != null) {
                            com.energyapp.App.bgMusicPlayer.setVolume(0.5);
                        }
                        rootLayout.setCenter(root);

                        // --- THE SUBSCRIPTION POP-UP JOKE ---
                        adCount++;
                        if (adCount == 2 && !isPlusSubscriber) {
                            javafx.application.Platform.runLater(() -> {
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Premium Offer");
                                alert.setHeaderText("Tired of seeing these ads?");
                                alert.setContentText("Subscribe to our convenient Plus Subscription plan for only $99.99/mo to only need to see the ad for 5 seconds to skip!");

                                javafx.scene.control.ButtonType subscribeBtn = new javafx.scene.control.ButtonType("Take my money!");
                                javafx.scene.control.ButtonType noThanksBtn = new javafx.scene.control.ButtonType("I love ads", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                                alert.getButtonTypes().setAll(subscribeBtn, noThanksBtn);

                                alert.showAndWait().ifPresent(type -> {
                                    if (type == subscribeBtn) {
                                        isPlusSubscriber = true; // They bought it! Next ad is 5 seconds.
                                        System.out.println("Valued subscriber acquired!");
                                    }
                                });
                            });
                        }
                        // ------------------------------------
                    });
                }
            } catch (Exception e) {
                rootLayout.setCenter(root);
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
