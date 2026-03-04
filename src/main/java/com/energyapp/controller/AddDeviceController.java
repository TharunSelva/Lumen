package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;
import com.energyapp.utils.DeviceFactory;
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

            if (DeviceFactory.createDevice(dName, selectedType) != null) {
                this.newlyCreatedDevice = DeviceFactory.createDevice(dName, selectedType);
            } else {
                System.out.println("this device to be implemented later");
            }
            ((Stage) addButton.getScene().getWindow()).close();
        });
    }

    public Device getNewlyCreatedDevice() {
        return this.newlyCreatedDevice;
    }

}
