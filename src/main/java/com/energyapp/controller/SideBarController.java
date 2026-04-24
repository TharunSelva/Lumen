package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.ViewType;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
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
    @FXML private VBox sidebarSearchSuggestionsBox;
    @FXML private ToggleButton sidebarDarkModeToggle;
    @FXML private VBox sideBarContainer;

    @FXML
    public void initialize() {
        updateSimulationClock(null);
        configureSearchSuggestions();
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

        SearchSuggestion firstSuggestion = firstSuggestionFor(sidebarSearchField.getText());
        if (firstSuggestion != null) {
            applySuggestion(firstSuggestion);
            return;
        }

        ViewType target = resolveSearchTarget(sidebarSearchField.getText());
        if (target == null) {
            sidebarSearchField.clear();
            sidebarSearchField.setPromptText(FALLBACK_SEARCH_PROMPT);
            hideSearchSuggestions();
            return;
        }

        sidebarSearchField.clear();
        sidebarSearchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        hideSearchSuggestions();
        routeTo(target);
    }

    List<String> suggestionTitlesFor(String rawQuery) {
        return buildSuggestions(rawQuery).stream()
                .map(SearchSuggestion::title)
                .toList();
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

    private void configureSearchSuggestions() {
        if (sidebarSearchField == null) {
            return;
        }

        sidebarSearchField.textProperty().addListener((obs, oldValue, newValue) -> updateSearchSuggestions(newValue));
        sidebarSearchField.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
            if (newFocused) {
                updateSearchSuggestions(sidebarSearchField.getText());
            } else {
                runLater(this::hideSearchSuggestions);
            }
        });
    }

    private void updateSearchSuggestions(String rawQuery) {
        if (sidebarSearchSuggestionsBox == null) {
            return;
        }

        List<SearchSuggestion> suggestions = buildSuggestions(rawQuery);
        sidebarSearchSuggestionsBox.getChildren().clear();

        if (suggestions.isEmpty()) {
            hideSearchSuggestions();
            return;
        }

        for (SearchSuggestion suggestion : suggestions) {
            sidebarSearchSuggestionsBox.getChildren().add(createSuggestionButton(suggestion));
        }

        sidebarSearchSuggestionsBox.setManaged(true);
        sidebarSearchSuggestionsBox.setVisible(true);
    }

    private void hideSearchSuggestions() {
        if (sidebarSearchSuggestionsBox == null) {
            return;
        }
        sidebarSearchSuggestionsBox.getChildren().clear();
        sidebarSearchSuggestionsBox.setManaged(false);
        sidebarSearchSuggestionsBox.setVisible(false);
    }

    private Button createSuggestionButton(SearchSuggestion suggestion) {
        Label titleLabel = new Label(suggestion.title());
        titleLabel.getStyleClass().add("sidebar-search-suggestion-title");

        Label subtitleLabel = new Label(suggestion.subtitle());
        subtitleLabel.getStyleClass().add("sidebar-search-suggestion-copy");
        subtitleLabel.setWrapText(true);

        VBox content = new VBox(2, titleLabel, subtitleLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("sidebar-search-suggestion");
        button.setOnAction(event -> applySuggestion(suggestion));
        return button;
    }

    private SearchSuggestion firstSuggestionFor(String rawQuery) {
        List<SearchSuggestion> suggestions = buildSuggestions(rawQuery);
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }

    private List<SearchSuggestion> buildSuggestions(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.UK);
        if (query.isBlank()) {
            return List.of();
        }

        List<SearchSuggestion> allSuggestions = List.of(
                new SearchSuggestion("Home", "Dashboard overview and live usage", ViewType.DASHBOARD, false,
                        List.of("home", "dashboard", "overview", "main")),
                new SearchSuggestion("Statistics", "Usage totals, costs, solar, and battery", ViewType.ENERGYSTATISTICS, false,
                        List.of("statistics", "stats", "usage", "cost", "energy")),
                new SearchSuggestion("Eco stats", "Carbon intensity, clean share, and eco trends", ViewType.ECOIMPACT, false,
                        List.of("eco", "carbon", "green", "sustain")),
                new SearchSuggestion("Forecasts", "Tomorrow solar, tariff, and planning windows", ViewType.FORECASTING, false,
                        List.of("forecast", "forecasts", "solar", "tariff", "tomorrow", "price", "battery")),
                new SearchSuggestion("Recommendations", "Live savings ideas and quick tips", ViewType.RECOMMENDATIONS, false,
                        List.of("recommendation", "recommendations", "tips", "budget", "saving", "advice")),
                new SearchSuggestion("Devices", "Device schedules, plans, and controls", ViewType.DEVICES, false,
                        List.of("device", "devices", "appliance", "plug", "washer")),
                new SearchSuggestion("Settings", "Profile, city, display, and accessibility", ViewType.SETTINGS, false,
                        List.of("settings", "profile", "city", "accessibility", "contrast")),
                new SearchSuggestion("Dark mode", "Toggle the app theme instantly", null, true,
                        List.of("dark", "theme", "night", "mode")),
                new SearchSuggestion("FAQ", "Open help and common Lumen questions", ViewType.SETTINGS, false,
                        List.of("faq", "help", "questions")),
                new SearchSuggestion("Report a Bug", "Open the bug-report tools in Settings", ViewType.SETTINGS, false,
                        List.of("bug", "issue", "problem", "report", "support"))
        );

        List<SearchSuggestion> matches = new ArrayList<>();
        for (SearchSuggestion suggestion : allSuggestions) {
            if (matchesQuery(suggestion, query)) {
                matches.add(suggestion);
            }
        }
        return matches.stream().limit(5).toList();
    }

    private boolean matchesQuery(SearchSuggestion suggestion, String query) {
        if (suggestion.title().toLowerCase(Locale.UK).contains(query)) {
            return true;
        }
        if (suggestion.subtitle().toLowerCase(Locale.UK).contains(query)) {
            return true;
        }
        return suggestion.keywords().stream().anyMatch(keyword -> keyword.contains(query) || query.contains(keyword));
    }

    private void applySuggestion(SearchSuggestion suggestion) {
        if (sidebarSearchField != null) {
            sidebarSearchField.clear();
            sidebarSearchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        }
        hideSearchSuggestions();

        if (suggestion.togglesDarkMode()) {
            if (sideBarContainer != null && sideBarContainer.getScene() != null) {
                ThemeController.setDarkMode(sideBarContainer.getScene(), !ThemeController.isDarkModeEnabled());
            }
            return;
        }

        routeTo(suggestion.target());
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

    private record SearchSuggestion(
            String title,
            String subtitle,
            ViewType target,
            boolean togglesDarkMode,
            List<String> keywords
    ) {
    }
}
