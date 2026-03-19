package com.energyapp.controller;

import com.energyapp.service.AccessibilityManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class TextOptionsController {

    @FXML private Button applyButton;
    @FXML private CheckBox boldTextCheckbox;
    @FXML private Button cancelButton;
    @FXML private ComboBox<String> fontComboBox;
    @FXML private Slider fontSizeSlider;
    @FXML private Label previewLabel;
    @FXML private Button restoreDefaultsButton;

    public void initialize() {
        fontComboBox.getItems().addAll(Font.getFamilies());
        fontSizeSlider.setValue(AccessibilityManager.customFontSizeProperty().getValue());
        boldTextCheckbox.setSelected(AccessibilityManager.customFontBoldProperty().getValue());

        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                updatePreview((Double) newVal, fontComboBox.getValue(), boldTextCheckbox.isSelected())
        );
        fontComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                updatePreview(fontSizeSlider.getValue(), newVal, boldTextCheckbox.isSelected())
        );
        boldTextCheckbox.selectedProperty().addListener((obs, oldVal, newVal) ->
                updatePreview(fontSizeSlider.getValue(), fontComboBox.getValue(), newVal)
        );

        fontComboBox.setValue(AccessibilityManager.customFontFamilyProperty().getValue());

        cancelButton.setOnAction(e -> {
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        applyButton.setOnAction(e -> {
            AccessibilityManager.setCustomFontBold(boldTextCheckbox.isSelected());
            AccessibilityManager.setCustomFontFamily(fontComboBox.getValue());
            AccessibilityManager.setFontSize(fontSizeSlider.getValue());

            ((Stage) applyButton.getScene().getWindow()).close();
        });

        restoreDefaultsButton.setOnAction(e -> {
            AccessibilityManager.setCustomFontFamily("System");
            AccessibilityManager.setCustomFontBold(false);
            AccessibilityManager.setFontSize(14.0);

            ((Stage) restoreDefaultsButton.getScene().getWindow()).close();
        });
    }

    private void updatePreview(double fontSize, String fontFamily, boolean isBold) {
        FontWeight weight;
        if (isBold) {
            weight = FontWeight.BOLD;
        } else {
            weight = FontWeight.NORMAL;
        }

        previewLabel.setFont(Font.font(fontFamily, weight, fontSize));
    }

}

