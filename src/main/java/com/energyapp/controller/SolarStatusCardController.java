package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class SolarStatusCardController {

    @FXML private ProgressBar generationProgressBar;
    @FXML private Button simulateFaultButton;
    @FXML private Label solarGenerationLabel;
    @FXML private Label solarStatusLabel;

    private boolean isSolarFaultActive = false;
    private SimulationService simulationService;
    private static final double MAX_SOLAR_CAPACITY = 4.0;


    public void setSimulationService(SimulationService service) {
        this.simulationService = service;

        simulationService.getCurrentEnergyRecord().addListener((obs, oldRec, newRec) -> {
            updateUI(newRec);
        });
    }

    @FXML
    public void initialize() {
        System.out.println("test: SolarStatusCardController successfully initialised");
        simulateFaultButton.setOnAction(event -> {
            isSolarFaultActive = !isSolarFaultActive;
            System.out.println("SOLAR FAULT ACTIVE: " + isSolarFaultActive);
        });
    }

    private void updateUI(EnergyRecord currentRecord) {
        // ui update will go here to change generation rate and progress
    }
}
