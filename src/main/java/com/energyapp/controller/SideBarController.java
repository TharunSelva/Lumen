package com.energyapp.controller;

import javafx.fxml.FXML;

public class SideBarController {

    private MainLayoutController mainController;

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void handleShowDashboard() {
        if (mainController != null) {
            mainController.handleShowDashboard();
        }
    }

    @FXML
    public void handleShowDevices() {
        if (mainController != null) {
            mainController.handleShowDevices();
        }
    }

    @FXML
    public void handleShowStatistics() {
        if (mainController != null) {
            mainController.handleShowStatistics();
        }
    }

    @FXML
    public void handleShowSettings() {
        if (mainController != null) {
            mainController.handleShowSettings();
        }
    }
    @FXML
    public void handleShowForecasting() {
        if (mainController != null) {
            mainController.handleShowForecasting();
        }
    }

}
