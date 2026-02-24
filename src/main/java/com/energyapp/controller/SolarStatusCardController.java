package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;

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
        Platform.runLater(() -> {
            double currentGen = EnergyCalculator.solarStatusCalc(currentRecord, MAX_SOLAR_CAPACITY).get(0);
            double progressBarValue = EnergyCalculator.solarStatusCalc(currentRecord, MAX_SOLAR_CAPACITY).get(1);
            generationProgressBar.setProgress(progressBarValue);
            solarGenerationLabel.setText(String.format("%.2f / %.1f kW", currentGen, MAX_SOLAR_CAPACITY));

            String solarStatus = EnergyCalculator.determineSolarGenLevel(currentRecord, progressBarValue);
            if (isSolarFaultActive) {
                solarStatusLabel.setText("Status: CRITICAL ERROR");
                solarStatusLabel.setTextFill(Color.RED);
            } else {
                switch (solarStatus) {
                    case "Status: Underperforming":
                        solarStatusLabel.setText(solarStatus);
                        solarStatusLabel.setTextFill(Color.ORANGE);

                    case "Status: Optimal":
                        solarStatusLabel.setText(solarStatus);
                        solarStatusLabel.setTextFill(Color.GREEN);
                }
            }
        });
    }
}
