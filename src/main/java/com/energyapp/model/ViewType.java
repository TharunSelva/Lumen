package com.energyapp.model;

public enum ViewType {

    DASHBOARD("/com/energyapp/view/Dashboard.fxml"),
    DEVICES("/com/energyapp/view/Devices.fxml"),
    SETTINGS("/com/energyapp/view/Settings.fxml"),
    ENERGYSTATISTICS("/com/energyapp/view/EnergyStatistics.fxml"),
    FORECASTING("/com/energyapp/view/Forecasting.fxml");

    private final String fxmlPath;

    ViewType(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

}
