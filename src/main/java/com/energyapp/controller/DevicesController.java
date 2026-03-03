package com.energyapp.controller;  // <--- Note the package name matches your folder

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;
import com.energyapp.service.DeviceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DevicesController {

    // --- Buttons ---
    @FXML private Button btnForAdd;

    @FXML private FlowPane deviceContainer;

    private DeviceService deviceService = new DeviceService();
    private List<Device> allDevices = deviceService.getAllDevices();

    @FXML
    public void initialize() {
        createDeviceButtons();
    }

    private void createDeviceButtons() {
        for (Device device : this.allDevices) {
            Button b = new Button(device.getDeviceName());
            b.setOnAction(event -> {
                try {
                    openDeviceSettings(device);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            deviceContainer.getChildren().add(b);
        }
    }

    private void openDeviceSettings(Device device) throws IOException {
        if (device.getDeviceType() == DeviceType.SMART_PLUG) {
            Stage settingsModalStage = new Stage();
            // setting as app modal means the user can't click the background app while the modal is active
            settingsModalStage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/SmartPlugSettings.fxml"));
            Parent settingsModalRoot = loader.load();
            SmartPlugSettingsController controller = loader.getController();
            controller.initData((SmartPlug) device);

            settingsModalStage.setTitle("Device Settings");
            settingsModalStage.setScene(new Scene(settingsModalRoot, 640, 360));

            settingsModalStage.showAndWait();
        } else {
            System.out.println("settings for this device to be implemented later");
        }
    }
}