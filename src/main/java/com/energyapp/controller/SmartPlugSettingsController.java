package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.SmartPlug;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class SmartPlugSettingsController {

    @FXML private TextField cutoffInput;
    @FXML private Label deviceNameTitle;
    @FXML private ToggleButton powerToggle;

    private SmartPlug currentPlug;

    public void initData(SmartPlug plug) {
        this.currentPlug = plug;
        deviceNameTitle.setText(plug.getDeviceName());
        powerToggle.selectedProperty().bindBidirectional(plug.isOnProperty());

        /* we use this to ensure there is no type mismatch between the String in the cutoffInput,
           and the double in priceCutoffProperty. */
        Bindings.bindBidirectional(cutoffInput.textProperty(), plug.priceCutoffProperty(),
                new javafx.util.converter.NumberStringConverter());

    }

}
