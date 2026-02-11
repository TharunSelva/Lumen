package com.energyapp.controller;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleGroup;

public class SideBarController {

    private MainLayoutController mainController;
    @FXML
    private ToggleButton devicesButton;

    @FXML
    private ToggleButton ecostatisticsButton;

    @FXML
    private ToggleButton forecastingButtons;

    @FXML
    private ToggleButton homeButton;

    @FXML
    private ToggleButton recommendationsButton;

    @FXML
    private ToggleButton settingsButton;

    @FXML
    private VBox sideBarContainer;

    @FXML
    private ToggleButton statisticsButton;

    private ToggleGroup menuGroup = new ToggleGroup();

    public SideBarController () {
        devicesButton.setToggleGroup(menuGroup);
        ecostatisticsButton.setToggleGroup(menuGroup);
        forecastingButtons.setToggleGroup(menuGroup);
        homeButton.setToggleGroup(menuGroup);
        recommendationsButton.setToggleGroup(menuGroup);
        settingsButton.setToggleGroup(menuGroup);
        homeButton.setSelected(true);
        menuGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
            }
        });
    }

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void handleShowDashboard() {
        if (mainController != null) {
            homeButton.setSelected(true);
            mainController.handleShowDashboard();
        }
    }

    @FXML
    public void handleShowDevices() {
        if (mainController != null) {
            devicesButton.setSelected(true);
            mainController.handleShowDevices();
        }
    }

    @FXML
    public void handleShowStatistics() {
        if (mainController != null) {
            statisticsButton.setSelected(true);
            mainController.handleShowStatistics();
        }
    }

    @FXML
    public void handleShowSettings() {
        if (mainController != null) {
            settingsButton.setSelected(true);
            mainController.handleShowSettings();
        }
    }
    @FXML
    public void handleShowForecasting() {
        if (mainController != null) {
            forecastingButtons.setSelected(true);
            mainController.handleShowForecasting();
        }
    }

}
