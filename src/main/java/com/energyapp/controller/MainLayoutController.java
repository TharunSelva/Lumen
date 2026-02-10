package com.energyapp.controller;

import com.energyapp.model.ViewType;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainLayoutController {

    @FXML private BorderPane rootLayout;
    @FXML private SideBarController mainSideBarController;
    @FXML private CostCardController weeklyCostCardController;

    private SimulationService simulationService;

    public void initialize() {
        System.out.println("Main layout controller successfully loaded");

        if (mainSideBarController != null) {
            mainSideBarController.setMainController(this);
        }

    }

    private void switchView(ViewType viewType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewType.getFxmlPath()));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardController) {
                DashboardController dc = (DashboardController) controller;
                dc.setMainController(this);                 // ✅ add this line
                dc.setSimService(this.simulationService);
            } else if (controller instanceof EnergyStatsController) {
                ((EnergyStatsController) controller).setSimulationService(this.simulationService);
            }

            rootLayout.setCenter(root);
        } catch (IOException e) {
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

}
