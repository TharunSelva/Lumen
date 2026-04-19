package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import com.energyapp.service.OpenMeteoSolarService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SetupModalController {

    @FXML private TextField cityTextField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private final HomeProfileRepository homeRepository = new HomeProfileRepository();
    private final OpenMeteoSolarService solarService = new OpenMeteoSolarService();

    @FXML
    public void initialize() {
        saveButton.setOnAction(event -> {
            String selectedLocation = cityTextField.getText();
            if (selectedLocation.equals("Enter City") || selectedLocation.isEmpty()) {
                errorLabel.setText("No valid city entered.");
                return;
            }

            try {
                Geocode code = solarService.geocoder(selectedLocation);
                homeRepository.updateHomeLocation("HOME_1", code);
                Stage stage = (Stage) saveButton.getScene().getWindow();
                stage.close();
            } catch (IllegalArgumentException | InterruptedException | IOException e) {
                errorLabel.setText("City not found. Try Again.");
                e.printStackTrace();
            }
        });
    }

}
