package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import com.energyapp.service.AccessibilityManager;
import com.energyapp.db.OpenMeteoSolarService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingsController {

    @FXML private ToggleButton settingsDisplaySetting1;   // Dark Mode
    @FXML private ToggleButton settingsDisplaySetting2;   // Compact View
    @FXML private ToggleButton settingsDisplaySetting3;   // Show Animations

    @FXML private ToggleButton settingsAccessibilitySetting1; // High Contrast
    @FXML private ToggleButton settingsAccessibilitySetting2; // Screen Reader
    @FXML private ToggleButton settingsAccessibilitySetting3; // Large Text
    @FXML private Button       settingsAccessibilityButton;   // Scalable Text Options

    @FXML private ToggleButton settingsPrivacySetting1; // Data Sharing
    @FXML private ToggleButton settingsPrivacySetting2; // Location Services
    @FXML private ToggleButton settingsPrivacySetting3; // Two-Factor Auth

    @FXML private TextField locationTextField;
    @FXML private Button    saveLocationButton;

    @FXML private Button settingsLogOutProceedButton;

    @FXML private VBox settingsMainVBox;

    @FXML
    public void initialize() {
        settingsMainVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                settingsDisplaySetting1.setSelected(ThemeController.isDarkModeEnabled());
                settingsAccessibilitySetting1.setSelected(ThemeController.isHighContrastEnabled());
                ThemeController.applyTheme(newScene);
                updateToggleLabels();
            }
        });

        settingsDisplaySetting1.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (settingsMainVBox.getScene() != null) {
                ThemeController.setDarkMode(settingsMainVBox.getScene(), isSelected);
            }
            updateToggleLabel(settingsDisplaySetting1);
        });

        settingsAccessibilitySetting1.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (settingsMainVBox.getScene() != null) {
                ThemeController.setHighContrast(settingsMainVBox.getScene(), isSelected);
            }
            updateToggleLabel(settingsAccessibilitySetting1);
        });

        settingsDisplaySetting2.selectedProperty().bindBidirectional(AccessibilityManager.isCompactView());
        settingsDisplaySetting3.selectedProperty().bindBidirectional(AccessibilityManager.animationsShowing());
        settingsAccessibilitySetting3.selectedProperty().bindBidirectional(AccessibilityManager.isLargeText());
        settingsAccessibilitySetting2.selectedProperty().bindBidirectional(AccessibilityManager.screenReaderProperty());

        settingsDisplaySetting2.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsDisplaySetting2));
        settingsDisplaySetting3.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsDisplaySetting3));
        settingsAccessibilitySetting2.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsAccessibilitySetting2));
        settingsAccessibilitySetting3.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsAccessibilitySetting3));
        settingsPrivacySetting1.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsPrivacySetting1));
        settingsPrivacySetting2.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsPrivacySetting2));
        settingsPrivacySetting3.selectedProperty().addListener((obs, o, n) -> updateToggleLabel(settingsPrivacySetting3));

        settingsAccessibilityButton.setOnAction(event -> showTextCustomizationModal());

        saveLocationButton.setOnAction(event -> {
            String selectedLocation = locationTextField.getText();
            if (selectedLocation.isBlank()) return;

            try {
                OpenMeteoSolarService solarService = new OpenMeteoSolarService();
                HomeProfileRepository profileRepository = new HomeProfileRepository();
                Geocode code = solarService.geocoder(selectedLocation);
                profileRepository.updateHomeLocation("HOME_1", code);
                locationTextField.clear();
            } catch (IllegalArgumentException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        settingsLogOutProceedButton.setOnAction(event -> handleExitApplication());
    }

    private void updateToggleLabels() {
        updateToggleLabel(settingsDisplaySetting1);
        updateToggleLabel(settingsDisplaySetting2);
        updateToggleLabel(settingsDisplaySetting3);
        updateToggleLabel(settingsAccessibilitySetting1);
        updateToggleLabel(settingsAccessibilitySetting2);
        updateToggleLabel(settingsAccessibilitySetting3);
        updateToggleLabel(settingsPrivacySetting1);
        updateToggleLabel(settingsPrivacySetting2);
        updateToggleLabel(settingsPrivacySetting3);
    }

    private void updateToggleLabel(ToggleButton btn) {
        btn.setText(btn.isSelected() ? "ON" : "OFF");
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

    public void handleExitApplication() {
        javafx.application.Platform.exit();
        System.exit(0);
    }
}