package com.energyapp.controller;

import com.energyapp.model.ViewType;
import com.energyapp.service.AccessibilityManager;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Platform.runLater;

public class MainLayoutController {
    @FXML private BorderPane rootLayout;
    @FXML private SideBarController mainSideBarController;

    private SimulationService simulationService;
    private ViewType currentView;

    public void initialize() {
        if (mainSideBarController != null) {
            mainSideBarController.setMainController(this);
        }

        AccessibilityManager.isLargeText().addListener((obs, wasLarge, isLarge) -> {
            if (isLarge) {
                if (!rootLayout.getStyleClass().contains("large-text")) {
                    rootLayout.getStyleClass().add("large-text");
                }
            } else {
                rootLayout.getStyleClass().remove("large-text");
            }
        });

        AccessibilityManager.isCompactView().addListener((obs, wasCompact, isCompact) -> {
            Stage stage = (Stage) rootLayout.getScene().getWindow();
            if (isCompact) {
                stage.setWidth(700.0);
                stage.setHeight(700.0);
                Node center = rootLayout.getCenter();
                if (center != null) {
                    applyCompactToHierarchy(center);
                }
                stage.setResizable(false);
                if (!rootLayout.getStyleClass().contains("compact-view")) {
                    rootLayout.getStyleClass().add("compact-view");
                }
            } else {
                stage.setWidth(1280.0);
                stage.setHeight(720.0);
                rootLayout.getStyleClass().remove("compact-view");
                stage.setResizable(true);
                switchView(currentView);
            }
        });

    }

    private void switchView (ViewType viewType){
        this.currentView = viewType;
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

            if (AccessibilityManager.isCompactView().get()) {
                runLater(() -> applyCompactToHierarchy(root));
            }
        rootLayout.setCenter(root);

        } catch(IOException e) {
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

    private void applyCompactToHierarchy(Node node) {
        if (node instanceof GridPane gp) {
            reflowGridPane(gp);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyCompactToHierarchy(child);
            }
        }

        if (node instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
            applyCompactToHierarchy(sp.getContent());
        }
    }

    private void reflowGridPane(GridPane gp) {
        gp.getColumnConstraints().clear();
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100);
        gp.getColumnConstraints().add(cc);

        int nextAvailableRow = 0;

        for (Node child : gp.getChildren()) {
            if (child.isManaged() && child.isVisible()) {
                GridPane.setRowIndex(child, nextAvailableRow);
                GridPane.setColumnIndex(child, 0);
                nextAvailableRow++;
            }
        }
    }
}
