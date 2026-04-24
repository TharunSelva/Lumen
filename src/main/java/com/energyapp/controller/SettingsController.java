package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.db.OpenMeteoSolarService;
import com.energyapp.model.Geocode;
import com.energyapp.service.AccessibilityManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SettingsController {
    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter BUG_REPORT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter BUG_REPORT_FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path BUG_REPORT_DIRECTORY =
            Path.of(System.getProperty("user.home"), "Lumen", "bug-reports");

    @FXML private VBox settingsMainVBox;
    @FXML private Label settingsGreetingLabel;
    @FXML private Label settingsProfileNameLabel;
    @FXML private Label settingsProfileLocationLabel;
    @FXML private TextField locationTextField;
    @FXML private Label locationStatusLabel;
    @FXML private Button saveLocationButton;
    @FXML private CheckBox darkModeCheckBox;
    @FXML private CheckBox compactViewCheckBox;
    @FXML private CheckBox animationsCheckBox;
    @FXML private CheckBox highContrastCheckBox;
    @FXML private CheckBox screenReaderCheckBox;
    @FXML private CheckBox largeTextCheckBox;
    @FXML private Button settingsAccessibilityButton;
    @FXML private Button faqButton;
    @FXML private Button reportBugButton;

    private final HomeProfileRepository homeProfileRepository = new HomeProfileRepository();
    private final OpenMeteoSolarService solarService = new OpenMeteoSolarService();

    @FXML
    public void initialize() {
        populateProfileDetails();
        configureThemeBindings();
        configureAccessibilityBindings();

        if (settingsAccessibilityButton != null) {
            settingsAccessibilityButton.setOnAction(event -> showTextCustomizationModal());
        }
        if (faqButton != null) {
            faqButton.setOnAction(event -> showFaqModal());
        }
        if (reportBugButton != null) {
            reportBugButton.setOnAction(event -> showBugReportModal());
        }
        if (saveLocationButton != null) {
            saveLocationButton.setOnAction(event -> handleSaveLocation());
        }
    }

    private void configureThemeBindings() {
        if (settingsMainVBox != null) {
            settingsMainVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    if (darkModeCheckBox != null) {
                        darkModeCheckBox.setSelected(ThemeController.isDarkModeEnabled());
                    }
                    if (highContrastCheckBox != null) {
                        highContrastCheckBox.setSelected(ThemeController.isHighContrastEnabled());
                    }
                    ThemeController.applyTheme(newScene);
                }
            });
        }

        if (darkModeCheckBox != null) {
            darkModeCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (settingsMainVBox != null && settingsMainVBox.getScene() != null) {
                    ThemeController.setDarkMode(settingsMainVBox.getScene(), isSelected);
                }
            });

            ThemeController.darkModeProperty().addListener((obs, oldValue, newValue) -> {
                if (darkModeCheckBox.isSelected() != newValue) {
                    darkModeCheckBox.setSelected(newValue);
                }
            });
        }

        if (highContrastCheckBox != null) {
            highContrastCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (settingsMainVBox != null && settingsMainVBox.getScene() != null) {
                    ThemeController.setHighContrast(settingsMainVBox.getScene(), isSelected);
                }
            });

            ThemeController.highContrastProperty().addListener((obs, oldValue, newValue) -> {
                if (highContrastCheckBox.isSelected() != newValue) {
                    highContrastCheckBox.setSelected(newValue);
                }
            });
        }
    }

    private void configureAccessibilityBindings() {
        if (compactViewCheckBox != null) {
            compactViewCheckBox.selectedProperty().bindBidirectional(AccessibilityManager.isCompactView());
        }
        if (animationsCheckBox != null) {
            animationsCheckBox.selectedProperty().bindBidirectional(AccessibilityManager.animationsShowing());
        }
        if (largeTextCheckBox != null) {
            largeTextCheckBox.selectedProperty().bindBidirectional(AccessibilityManager.isLargeText());
        }
        if (screenReaderCheckBox != null) {
            screenReaderCheckBox.selectedProperty().bindBidirectional(AccessibilityManager.screenReaderProperty());
        }
    }

    private void handleSaveLocation() {
        String selectedLocation = normalize(locationTextField == null ? null : locationTextField.getText());
        if (selectedLocation.isEmpty()) {
            showLocationStatus("Enter your city to personalise forecasts and solar estimates.", false);
            restoreSavedCity();
            return;
        }

        try {
            Geocode code = solarService.geocoder(selectedLocation);
            homeProfileRepository.updateHomeCity(HOME_ID, selectedLocation, code);
            populateProfileDetails();
            showLocationStatus("Saved " + selectedLocation + " for forecasts and solar estimates.", true);
        } catch (IllegalArgumentException e) {
            showLocationStatus("We couldn't find that city. Try a nearby town or city name.", false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showLocationStatus("We couldn't update your city right now. Please try again.", false);
        } catch (IOException e) {
            showLocationStatus("We couldn't reach the location service right now. Please try again.", false);
        }
    }

    private void restoreSavedCity() {
        if (locationTextField == null) {
            return;
        }

        HomeProfileRepository.HomeProfileSnapshot profile = homeProfileRepository.getProfile(HOME_ID);
        locationTextField.setText(profile == null || profile.cityName() == null ? "" : profile.cityName());
    }

    private void populateProfileDetails() {
        HomeProfileRepository.HomeProfileSnapshot profile = homeProfileRepository.getProfile(HOME_ID);
        String firstName = profile == null ? null : profile.firstName();
        String cityName = profile == null ? null : profile.cityName();

        if (settingsGreetingLabel != null) {
            settingsGreetingLabel.setText(buildSettingsGreeting(firstName));
        }
        if (settingsProfileNameLabel != null) {
            settingsProfileNameLabel.setText(normalize(firstName).isEmpty() ? "Your home profile" : firstName.trim());
        }
        if (settingsProfileLocationLabel != null) {
            settingsProfileLocationLabel.setText(buildProfileLocation(cityName));
        }
        if (locationTextField != null) {
            locationTextField.setText(cityName == null ? "" : cityName);
        }
        clearLocationStatus();
    }

    static String buildSettingsGreeting(String firstName) {
        String cleanedName = normalize(firstName);
        return cleanedName.isEmpty()
                ? "Lumen settings"
                : "Lumen settings for " + cleanedName;
    }

    static String buildProfileLocation(String cityName) {
        String cleanedCity = normalize(cityName);
        return cleanedCity.isEmpty()
                ? "Add your city to personalise forecasts, solar outlooks, and recommendations."
                : "Home city: " + cleanedCity + ". Forecasts and solar estimates use this location.";
    }

    static List<FaqEntry> faqEntries() {
        return List.of(
                new FaqEntry(
                        "How does the dashboard decide what to show?",
                        "The home screen pulls together your current usage view, forecast highlights, eco impact, and device summaries so you can scan the state of your home quickly."
                ),
                new FaqEntry(
                        "Where do forecast and solar insights come from?",
                        "Lumen uses your saved city to geocode your home location, then uses that location for forecast summaries and solar outlook data."
                ),
                new FaqEntry(
                        "What can I manage from Devices?",
                        "Devices lets you review what is connected, see what is currently running, and update schedules without leaving the main app flow."
                ),
                new FaqEntry(
                        "What accessibility features are available?",
                        "You can switch dark mode, high contrast, compact view, large text, screen reader support, animation visibility, and scalable text settings."
                ),
                new FaqEntry(
                        "What should I include in a bug report?",
                        "A strong report includes what you were doing, what you expected, what happened instead, and which page you were on such as Home, Forecasts, Recommendations, Devices, or Settings."
                )
        );
    }

    static String buildBugReportTemplate(HomeProfileRepository.HomeProfileSnapshot profile, LocalDateTime timestamp) {
        String firstName = profile == null ? "" : normalize(profile.firstName());
        String cityName = profile == null ? "" : normalize(profile.cityName());

        return "Lumen bug report\n"
                + "Created: " + timestamp.format(BUG_REPORT_TIME_FORMAT) + "\n"
                + "Profile: " + (firstName.isEmpty() ? "Not set" : firstName) + "\n"
                + "Saved city: " + (cityName.isEmpty() ? "Not set" : cityName) + "\n"
                + "Dark mode: " + (ThemeController.isDarkModeEnabled() ? "On" : "Off") + "\n"
                + "High contrast: " + (ThemeController.isHighContrastEnabled() ? "On" : "Off") + "\n"
                + "\n"
                + "What happened?\n"
                + "\n"
                + "\n"
                + "What did you expect instead?\n"
                + "\n"
                + "\n"
                + "Which page were you using?\n"
                + "Home / Statistics / Eco stats / Forecasts / Recommendations / Devices / Settings\n"
                + "\n"
                + "Steps to reproduce\n"
                + "1. \n"
                + "2. \n"
                + "3. \n";
    }

    public void showTextCustomizationModal() {
        Stage textOptionsModalStage = new Stage();
        textOptionsModalStage.initModality(Modality.APPLICATION_MODAL);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/TextOptionsModal.fxml"));
        Parent textOptionsModalRoot;
        try {
            textOptionsModalRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        textOptionsModalStage.setTitle("Text Options");
        Scene textOptionsScene = new Scene(textOptionsModalRoot, 400, 250);
        ThemeController.applyTheme(textOptionsScene);
        textOptionsModalStage.setScene(textOptionsScene);
        textOptionsModalStage.showAndWait();
    }

    private void showFaqModal() {
        VBox content = new VBox(14);
        content.getStyleClass().add("settings-modal-root");

        Label titleLabel = new Label("Lumen FAQ");
        titleLabel.getStyleClass().add("settings-modal-title");

        Label introLabel = new Label("Answers below are based on the features currently available in the app.");
        introLabel.setWrapText(true);
        introLabel.getStyleClass().add("settings-modal-copy");

        content.getChildren().addAll(titleLabel, introLabel);
        for (FaqEntry entry : faqEntries()) {
            content.getChildren().add(createFaqCard(entry));
        }

        Stage modal = createModalStage("Lumen FAQ", content, 620, 560);
        modal.showAndWait();
    }

    private void showBugReportModal() {
        HomeProfileRepository.HomeProfileSnapshot profile = homeProfileRepository.getProfile(HOME_ID);

        VBox content = new VBox(14);
        content.getStyleClass().add("settings-modal-root");

        Label titleLabel = new Label("Report a bug");
        titleLabel.getStyleClass().add("settings-modal-title");

        Label introLabel = new Label("Describe the issue and either copy the report or save a draft for later.");
        introLabel.setWrapText(true);
        introLabel.getStyleClass().add("settings-modal-copy");

        TextArea reportTextArea = new TextArea(buildBugReportTemplate(profile, LocalDateTime.now()));
        reportTextArea.setWrapText(true);
        reportTextArea.setPrefRowCount(14);
        reportTextArea.getStyleClass().add("settings-report-area");
        VBox.setVgrow(reportTextArea, Priority.ALWAYS);

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("settings-status-label");

        HBox actionsRow = new HBox(10);
        Button copyButton = new Button("Copy report");
        copyButton.getStyleClass().add("settings-action-button");

        Button saveDraftButton = new Button("Save draft");
        saveDraftButton.getStyleClass().add("settings-action-button");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().addAll("settings-action-button", "settings-action-button-secondary");
        actionsRow.getChildren().addAll(copyButton, saveDraftButton, closeButton);

        content.getChildren().addAll(titleLabel, introLabel, reportTextArea, feedbackLabel, actionsRow);

        Stage modal = createModalStage("Report a bug", content, 680, 560);

        copyButton.setOnAction(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(reportTextArea.getText());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            updateStatusLabel(feedbackLabel, "Copied your report to the clipboard.", true);
        });

        saveDraftButton.setOnAction(event -> {
            try {
                Path savedReport = saveBugReport(reportTextArea.getText(), LocalDateTime.now());
                updateStatusLabel(feedbackLabel, "Saved draft to " + savedReport.toAbsolutePath() + ".", true);
            } catch (IOException e) {
                updateStatusLabel(feedbackLabel, "We couldn't save the draft right now. Please try again.", false);
            }
        });

        closeButton.setOnAction(event -> modal.close());
        modal.showAndWait();
    }

    private VBox createFaqCard(FaqEntry entry) {
        VBox card = new VBox(6);
        card.getStyleClass().add("settings-faq-card");

        Label questionLabel = new Label(entry.question());
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("settings-faq-question");

        Label answerLabel = new Label(entry.answer());
        answerLabel.setWrapText(true);
        answerLabel.getStyleClass().add("settings-faq-answer");

        card.getChildren().addAll(questionLabel, answerLabel);
        return card;
    }

    private Stage createModalStage(String title, Parent content, double width, double height) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle(title);

        Scene modalScene = new Scene(content, width, height);
        ThemeController.applyTheme(modalScene);
        modalStage.setScene(modalScene);
        return modalStage;
    }

    private Path saveBugReport(String reportBody, LocalDateTime timestamp) throws IOException {
        Files.createDirectories(BUG_REPORT_DIRECTORY);
        Path target = BUG_REPORT_DIRECTORY.resolve("lumen-bug-report-" + timestamp.format(BUG_REPORT_FILE_FORMAT) + ".txt");
        return Files.writeString(target, reportBody == null ? "" : reportBody.trim() + System.lineSeparator());
    }

    private void showLocationStatus(String message, boolean success) {
        updateStatusLabel(locationStatusLabel, message, success);
    }

    private void clearLocationStatus() {
        if (locationStatusLabel == null) {
            return;
        }
        locationStatusLabel.setText("");
        locationStatusLabel.getStyleClass().removeAll("settings-status-success", "settings-status-error");
    }

    private void updateStatusLabel(Label label, String message, boolean success) {
        if (label == null) {
            return;
        }

        label.setText(message);
        label.getStyleClass().removeAll("settings-status-success", "settings-status-error");
        label.getStyleClass().add(success ? "settings-status-success" : "settings-status-error");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    record FaqEntry(String question, String answer) {
    }
}
