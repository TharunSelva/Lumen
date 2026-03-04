package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.SmartPlug;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class SmartPlugSettingsController {

    @FXML private TextField cutoffInput;
    @FXML private Label deviceNameTitle;
    @FXML private ToggleButton powerToggle;
    @FXML private Button applyButton;

    private SmartPlug currentPlug;

    public void initData(SmartPlug plug) {
        this.currentPlug = plug;
        deviceNameTitle.setText(plug.getDeviceName());
        powerToggle.textProperty().bind(Bindings.when(powerToggle.selectedProperty()).then("ON").otherwise("OFF"));
        powerToggle.setSelected(plug.isOn());
        cutoffInput.setText(String.valueOf(plug.getPriceCutoff()));

        applyButton.setOnAction((event) -> {
            currentPlug.setOn(powerToggle.isSelected());
            currentPlug.setPriceCutoff(Double.parseDouble(cutoffInput.getText()));
            ((javafx.stage.Stage) applyButton.getScene().getWindow()).close();
        });
    }

}
