package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.utils.DeviceFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.stage.Stage;

import java.time.LocalTime;

public class AddDeviceController {

    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private ComboBox<DeviceType> selectDeviceType;
    @FXML private TextField deviceName;
    @FXML private Label errorLabel;

    private Device newlyCreatedDevice = null;

    @FXML
    public void initialize() {
        selectDeviceType.getItems().addAll(DeviceType.values());
        selectDeviceType.setConverter(new StringConverter<>() {
            @Override
            public String toString(DeviceType deviceType) {
                return deviceType == null ? "" : deviceType.name().replace('_', ' ');
            }

            @Override
            public DeviceType fromString(String string) {
                return null;
            }
        });

        cancelButton.setOnAction((event) -> {
            if (cancelButton.getScene() != null && cancelButton.getScene().getWindow() instanceof Stage stage) {
                stage.close();
            }
        });

        addButton.setOnAction((event) -> {
            String dName = deviceName.getText() == null ? "" : deviceName.getText().trim();
            DeviceType selectedType = selectDeviceType.getValue();

            if (dName.isBlank()) {
                errorLabel.setText("Enter a device name before adding it.");
                return;
            }

            if (selectedType == null) {
                errorLabel.setText("Choose a device type to continue.");
                return;
            }

            Device device = DeviceFactory.createDevice(dName, selectedType);
            if (device == null) {
                errorLabel.setText("This device type is not available yet.");
                return;
            }

            applyReasonableDefaults(device);
            this.newlyCreatedDevice = device;
            if (addButton.getScene() != null && addButton.getScene().getWindow() instanceof Stage stage) {
                stage.close();
            }
        });
    }

    public Device getNewlyCreatedDevice() {
        return this.newlyCreatedDevice;
    }

    private void applyReasonableDefaults(Device device) {
        if (device instanceof SmartPlug smartPlug) {
            smartPlug.setPriceCutoff(22.5);
            smartPlug.setOn(false);
            smartPlug.setStatus(DeviceStatus.OFF);
            return;
        }

        if (device instanceof ScheduledDevice scheduledDevice) {
            scheduledDevice.setOn(false);
            scheduledDevice.setSchedule(defaultSchedule(device.getDeviceType()));
            scheduledDevice.setStatus(DeviceStatus.SCHEDULED);
            return;
        }

        if (device.getDeviceType() == DeviceType.FRIDGE) {
            device.setOn(true);
            device.setStatus(DeviceStatus.IDLE);
            return;
        }

        device.setOn(false);
        device.setStatus(DeviceStatus.OFF);
    }

    private LocalTime defaultSchedule(DeviceType deviceType) {
        return switch (deviceType) {
            case WASHING_MACHINE -> LocalTime.of(10, 30);
            case DISHWASHER -> LocalTime.of(13, 0);
            case EV_CHARGER -> LocalTime.of(0, 30);
            default -> LocalTime.of(18, 0);
        };
    }

}
