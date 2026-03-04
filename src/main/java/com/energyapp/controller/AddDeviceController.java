package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddDeviceController {

    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private ComboBox<DeviceType> selectDeviceType;
    @FXML private TextField deviceName;

    private Device newlyCreatedDevice = null;

    @FXML
    public void initialize() {
        selectDeviceType.getItems().addAll(DeviceType.values());

        cancelButton.setOnAction((event) -> {
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        addButton.setOnAction((event) -> {
            String dName = deviceName.getText();
            DeviceType selectedType = selectDeviceType.getValue();

            if (selectedType == DeviceType.SMART_PLUG) {
                this.newlyCreatedDevice = new SmartPlug(dName, selectedType);
            } else {
                System.out.println("adding this device type to be implemented later");
                // implement logic to add other devices like washing machine and fridge here
            }
            ((Stage) addButton.getScene().getWindow()).close();
        });
    }

    public Device getNewlyCreatedDevice() {
        return this.newlyCreatedDevice;
    }

}
