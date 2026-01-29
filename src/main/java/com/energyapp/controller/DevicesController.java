package com.energyapp.controller;  // <--- Note the package name matches your folder

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.control.TitledPane;

public class DevicesController {

    // --- Buttons ---
    @FXML private Button btnForDevice1;
    @FXML private Button btnForDevice2;
    @FXML private Button btnForDevice3;
    @FXML private Button btnForDevice4;
    @FXML private Button btnForDevice5;
    @FXML private Button btnForAdd;

    // --- Images ---
    @FXML private ImageView device1Image;
    @FXML private ImageView device2Image;
    @FXML private ImageView device3Image;
    @FXML private ImageView device4Image;
    @FXML private ImageView device5Image;
    @FXML private ImageView addDeviceImage;

    // --- Titled Panes ---
    @FXML private TitledPane device1;
    @FXML private TitledPane device2;
    @FXML private TitledPane device3;
    @FXML private TitledPane device4;
    @FXML private TitledPane device5;

    @FXML
    public void initialize() {
        System.out.println("DevicesController has been loaded successfully.");

        // Example: Add a click action to the LG Washer (Device 1)
        btnForDevice1.setOnAction(event -> {
            System.out.println("You clicked the LG Washer!");
        });

        // Example: Add a click action to the "Add Device" button
        btnForAdd.setOnAction(event -> {
            System.out.println("Add Device button clicked");
        });
    }
}