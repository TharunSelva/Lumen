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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingsController {

    @FXML private Button settingsAccessibilityButton;
    @FXML private GridPane settingsAccessibilityGridPane;
    @FXML private CheckBox settingsAccessibilitySetting1;
    @FXML private CheckBox settingsAccessibilitySetting2;
    @FXML private CheckBox settingsAccessibilitySetting3;
    @FXML private VBox settingsAccessibilitySettingsContentsVBox;
    @FXML private Label settingsAccessibilityTitleLabel;
    @FXML private VBox settingsAccessibilityVBox;
    @FXML private VBox settingsAccountContentsVBox;
    @FXML private CheckBox settingsAccountSetting1;
    @FXML private CheckBox settingsAccountSetting2;
    @FXML private CheckBox settingsAccountSetting3;
    @FXML private Label settingsAccountTitleLabel;
    @FXML private VBox settingsAccountVBox;
    @FXML private CheckBox settingsDisplaySetting2;
    @FXML private CheckBox settingsDisplaySetting3;
    @FXML private Label settingsDisplayTitleLabel;
    @FXML private VBox settingsDisplayVBox;
    @FXML private Label settingsGreetingLabel;
    @FXML private CheckBox settingsHelpSetting1;
    @FXML private CheckBox settingsHelpSetting2;
    @FXML private CheckBox settingsHelpSetting3;
    @FXML private Label settingsHelpTitleLabel;
    @FXML private VBox settingsHelpVBox;
    @FXML private Label settingsLogOutMessageLabel;
    @FXML private Button settingsLogOutProceedButton;
    @FXML private Label settingsLogOutTitleLabel;
    @FXML private VBox settingsLogOutVBox;
    @FXML private GridPane settingsMainGridPane;
    @FXML private VBox settingsMainVBox;
    @FXML private CheckBox settingsPrivacySetting1;
    @FXML private CheckBox settingsPrivacySetting2;
    @FXML private CheckBox settingsPrivacySetting3;
    @FXML private Label settingsPrivacyTitleLabel;
    @FXML private VBox settingsPrivacyVBox;
    @FXML private CheckBox settingsDisplaySetting1;   // Dark Mode
    @FXML private TextField locationTextField;
    @FXML private Button saveLocationButton;

    @FXML
    public void initialize() {
        settingsMainVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                settingsDisplaySetting1.setSelected(ThemeController.isDarkModeEnabled());
                settingsAccessibilitySetting1.setSelected(ThemeController.isHighContrastEnabled());
                ThemeController.applyTheme(newScene);
            }
        });

        settingsDisplaySetting1.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (settingsMainVBox.getScene() != null) {
                ThemeController.setDarkMode(settingsMainVBox.getScene(), isSelected);
            }
        });

        settingsAccessibilitySetting1.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (settingsMainVBox.getScene() != null) {
                ThemeController.setHighContrast(settingsMainVBox.getScene(), isSelected);
            }
        });

        settingsDisplaySetting2.selectedProperty().bindBidirectional(AccessibilityManager.isCompactView());
        settingsDisplaySetting3.selectedProperty().bindBidirectional(AccessibilityManager.animationsShowing());
        settingsAccessibilitySetting3.selectedProperty().bindBidirectional(AccessibilityManager.isLargeText());

        settingsAccessibilityButton.setOnAction(event -> {
                showTextCustomizationModal();
        });

        settingsAccessibilitySetting2.selectedProperty().bindBidirectional(AccessibilityManager.screenReaderProperty());

        saveLocationButton.setOnAction(event -> {
            String selectedLocation = locationTextField.getText();
            if (selectedLocation.equals("Enter City") || selectedLocation.isEmpty()) {
                return;
            }

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



















}