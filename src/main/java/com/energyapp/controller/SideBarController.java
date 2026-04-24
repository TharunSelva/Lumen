package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.ViewType;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static javafx.application.Platform.runLater;

public class SideBarController {
    private static final String DEFAULT_SEARCH_PROMPT = "Search pages or actions";
    private static final String FALLBACK_SEARCH_PROMPT = "Try home, forecasts, devices or FAQ";
    private static final DateTimeFormatter SIMULATION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SIMULATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.UK);

    private MainLayoutController mainController;
    private boolean simulationListenerAttached;

    @FXML private Label simulationTimeLabel;
    @FXML private Label simulationDateLabel;
    @FXML private TextField sidebarSearchField;
    @FXML private ToggleButton sidebarDarkModeToggle;
    @FXML private VBox sideBarContainer;

    @FXML
    public void initialize() {
        updateSimulationClock(null);
        configureThemeToggle();
    }

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    public void setSimulationService(SimulationService simulationService) {
        updateSimulationClock(simulationService == null ? null : simulationService.getCurrentEnergyRecord().get());
        if (simulationService != null && !simulationListenerAttached) {
            simulationService.getCurrentEnergyRecord().addListener((obs, oldRecord, newRecord) ->
                    updateSimulationClock(newRecord));
            simulationListenerAttached = true;
        }
    }

    private void updateSimulationClock(EnergyRecord record) {
        runLater(() -> {
            if (simulationTimeLabel == null || simulationDateLabel == null) {
                return;
            }

            if (record == null || record.getTimestamp() == null) {
                simulationTimeLabel.setText("--:--");
                simulationDateLabel.setText("Waiting for simulation data");
                return;
            }

            simulationTimeLabel.setText(record.getTimestamp().format(SIMULATION_TIME_FORMAT));
            simulationDateLabel.setText(record.getTimestamp().format(SIMULATION_DATE_FORMAT));
        });
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

    @FXML
    public void handleShowRecommendations() {
        if (mainController != null) {
            mainController.handleShowRecommendations();
        }
    }

    @FXML
    public void handleShowEcoStatistics() {
        if (mainController != null) {
            mainController.handleShowEcoStatistics();
        }
    }

    @FXML
    public void handleSidebarSearch() {
        if (sidebarSearchField == null) {
            return;
        }

        ViewType target = resolveSearchTarget(sidebarSearchField.getText());
        if (target == null) {
            sidebarSearchField.clear();
            sidebarSearchField.setPromptText(FALLBACK_SEARCH_PROMPT);
            return;
        }

        sidebarSearchField.clear();
        sidebarSearchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        routeTo(target);
    }

    ViewType resolveSearchTarget(String rawQuery) {
        if (rawQuery == null) {
            return null;
        }

        String query = rawQuery.trim().toLowerCase(Locale.UK);
        if (query.isEmpty()) {
            return null;
        }

        if (containsAny(query, "home", "dashboard", "overview")) {
            return ViewType.DASHBOARD;
        }
        if (containsAny(query, "statistics", "stats", "usage", "cost", "energy")) {
            return ViewType.ENERGYSTATISTICS;
        }
        if (containsAny(query, "eco", "carbon", "green", "sustain")) {
            return ViewType.ECOIMPACT;
        }
        if (containsAny(query, "forecast", "forecasting", "solar", "tariff", "price", "tomorrow", "battery")) {
            return ViewType.FORECASTING;
        }
        if (containsAny(query, "recommend", "tip", "tips", "advice", "budget")) {
            return ViewType.RECOMMENDATIONS;
        }
        if (containsAny(query, "device", "devices", "appliance", "plug", "washer")) {
            return ViewType.DEVICES;
        }
        if (containsAny(query, "settings", "theme", "accessibility", "profile", "setup",
                "faq", "help", "support", "bug", "contrast", "dark")) {
            return ViewType.SETTINGS;
        }

        return null;
    }

    private boolean containsAny(String query, String... keywords) {
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void routeTo(ViewType target) {
        if (mainController == null || target == null) {
            return;
        }

        switch (target) {
            case DASHBOARD -> mainController.handleShowDashboard();
            case ENERGYSTATISTICS -> mainController.handleShowStatistics();
            case ECOIMPACT -> mainController.handleShowEcoStatistics();
            case FORECASTING -> mainController.handleShowForecasting();
            case RECOMMENDATIONS -> mainController.handleShowRecommendations();
            case DEVICES -> mainController.handleShowDevices();
            case SETTINGS -> mainController.handleShowSettings();
        }
    }

    private void configureThemeToggle() {
        if (sidebarDarkModeToggle == null) {
            return;
        }

        sidebarDarkModeToggle.setSelected(ThemeController.isDarkModeEnabled());
        sidebarDarkModeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (sideBarContainer != null && sideBarContainer.getScene() != null) {
                ThemeController.setDarkMode(sideBarContainer.getScene(), isSelected);
            }
        });

        ThemeController.darkModeProperty().addListener((obs, oldValue, newValue) -> {
            if (sidebarDarkModeToggle.isSelected() != newValue) {
                sidebarDarkModeToggle.setSelected(newValue);
            }
        });

        if (sideBarContainer != null) {
            sideBarContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    sidebarDarkModeToggle.setSelected(ThemeController.isDarkModeEnabled());
                    ThemeController.applyTheme(newScene);
                }
            });
        }
    }
}
