package com.energyapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

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

    @FXML
    public void initialize() {
        settingsMainVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                settingsDisplaySetting1.setSelected(ThemeController.isDarkModeEnabled());
                ThemeController.applyTheme(newScene);
            }
        });

        settingsDisplaySetting1.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (settingsMainVBox.getScene() != null) {
                ThemeController.setDarkMode(settingsMainVBox.getScene(), isSelected);
            }
        });

        System.out.println("Settings controller successfully loaded.");
    }
}