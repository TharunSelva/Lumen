package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.db.OpenMeteoSolarService;
import com.energyapp.model.Geocode;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SetupModalController {
    private static final String HOME_ID = "HOME_1";

    @FXML private TextField firstNameTextField;
    @FXML private TextField cityTextField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private final HomeProfileRepository homeRepository = new HomeProfileRepository();
    private final OpenMeteoSolarService solarService = new OpenMeteoSolarService();

    @FXML
    public void initialize() {
        populateExistingValues(loadExistingProfile());
        hideError();
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(event -> handleSave());
    }

    private void populateExistingValues(HomeProfileRepository.HomeProfileSnapshot profile) {
        if (profile == null) {
            return;
        }

        if (profile.firstName() != null && !profile.firstName().isBlank()) {
            firstNameTextField.setText(profile.firstName());
        }
        if (profile.cityName() != null && !profile.cityName().isBlank()) {
            cityTextField.setText(profile.cityName());
        }
    }

    private void handleSave() {
        hideError();

        String firstName = normalize(firstNameTextField.getText());
        String cityName = normalize(cityTextField.getText());
        String validationMessage = validateProfileInput(firstName, cityName);
        if (validationMessage != null) {
            showError(validationMessage);
            return;
        }

        saveButton.setDisable(true);
        try {
            Geocode code = geocodeCity(cityName);
            saveProfile(firstName, cityName, code);
            closeWindow();
        } catch (IllegalArgumentException e) {
            showError("We couldn't find that city yet. Try a nearby city name.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showError("We couldn't finish setup right now. Please try again.");
        } catch (IOException | IllegalStateException e) {
            showError("We couldn't save your setup right now. Please try again.");
            e.printStackTrace();
        } finally {
            saveButton.setDisable(false);
        }
    }

    static String validateProfileInput(String firstName, String cityName) {
        if (firstName.isEmpty() && cityName.isEmpty()) {
            return "Please enter your first name and city.";
        }
        if (firstName.isEmpty()) {
            return "Please enter your first name.";
        }
        if (cityName.isEmpty()) {
            return "Please enter your city.";
        }
        return null;
    }

    protected HomeProfileRepository.HomeProfileSnapshot loadExistingProfile() {
        return homeRepository.getProfile(HOME_ID);
    }

    protected Geocode geocodeCity(String cityName) throws IOException, InterruptedException {
        return solarService.geocoder(cityName);
    }

    protected void saveProfile(String firstName, String cityName, Geocode code) {
        homeRepository.saveProfile(HOME_ID, firstName, cityName, code);
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
