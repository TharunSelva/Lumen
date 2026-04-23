package com.energyapp.controller;

import com.energyapp.db.HomeEnergyAssetsRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class SolarStatusCardController {

    private static final double DEFAULT_SOLAR_CAPACITY_KW = 4.0;
    private static final String BASE_STATUS_STYLE = "solar-status-pill";

    @FXML private ProgressBar generationProgressBar;
    @FXML private Button simulateFaultButton;
    @FXML private Label solarGenerationLabel;
    @FXML private Label solarStatusLabel;
    @FXML private Label solarCapacityLabel;
    @FXML private Label diagnosticHelperLabel;

    private boolean isSolarFaultActive = false;
    private SimulationService simulationService;
    private ChangeListener<EnergyRecord> currentRecordListener;
    private HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets =
            HomeEnergyAssetsRepository.HomeEnergyAssets.defaults();

    @FXML
    public void initialize() {
        simulateFaultButton.setOnAction(event -> {
            isSolarFaultActive = !isSolarFaultActive;
            updateDiagnosticButtonState();
            refreshFromCurrentData();
        });

        updateDiagnosticButtonState();
        updateUnavailableState();
    }

    public void setSimulationService(SimulationService service) {
        if (this.simulationService != null && currentRecordListener != null) {
            this.simulationService.getCurrentEnergyRecord().removeListener(currentRecordListener);
        }

        this.simulationService = service;

        if (this.simulationService != null) {
            currentRecordListener = (obs, oldRec, newRec) -> {
                if (newRec != null) {
                    updateUI(newRec);
                }
            };
            this.simulationService.getCurrentEnergyRecord().addListener(currentRecordListener);
        }

        refreshFromCurrentData();
    }

    public void setHomeEnergyAssets(HomeEnergyAssetsRepository.HomeEnergyAssets assets) {
        this.homeEnergyAssets = assets != null
                ? assets
                : HomeEnergyAssetsRepository.HomeEnergyAssets.defaults();
        updateDiagnosticButtonState();
        refreshFromCurrentData();
    }

    private void refreshFromCurrentData() {
        EnergyRecord record = getLatestAvailableRecord();
        if (record != null) {
            updateUI(record);
        } else {
            updateUnavailableState();
        }
    }

    private EnergyRecord getLatestAvailableRecord() {
        if (simulationService == null) {
            return null;
        }

        EnergyRecord current = simulationService.getCurrentEnergyRecord().get();
        if (current != null) {
            return current;
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    private void updateUI(EnergyRecord currentRecord) {
        Runnable refreshAction = () -> {
            if (!homeEnergyAssets.hasSolar()) {
                updateNotConfiguredState();
                return;
            }

            double solarCapacityKw = homeEnergyAssets.solarCapacityOrDefault(DEFAULT_SOLAR_CAPACITY_KW);
            double currentSolarKw = solarKwFromHalfHourKwh(currentRecord.getSolarGenKwh());
            double progressValue = capacityProgress(currentSolarKw, solarCapacityKw);

            generationProgressBar.setProgress(progressValue);
            solarGenerationLabel.setText(String.format("%.1f kW live output", currentSolarKw));
            solarCapacityLabel.setText(String.format("%.0f%% of %.1f kW capacity", progressValue * 100.0, solarCapacityKw));
            diagnosticHelperLabel.setText(
                    isSolarFaultActive
                            ? "Demo diagnostic state is active for this solar card."
                            : "Run a demo diagnostic to simulate a panel alert state."
            );

            if (isSolarFaultActive) {
                setStatus("Status: Diagnostic alert active", "status-pill-alert");
                return;
            }

            String solarStatus = EnergyCalculator.determineSolarGenLevel(currentRecord, progressValue);
            setStatus(solarStatus, statusStyleClassFor(solarStatus));
        };

        if (Platform.isFxApplicationThread()) {
            refreshAction.run();
        } else {
            Platform.runLater(refreshAction);
        }
    }

    private void updateUnavailableState() {
        if (homeEnergyAssets.hasSolar()) {
            generationProgressBar.setProgress(0.0);
            solarGenerationLabel.setText("0.0 kW live output");
            solarCapacityLabel.setText(String.format("0%% of %.1f kW capacity", homeEnergyAssets.solarCapacityOrDefault(DEFAULT_SOLAR_CAPACITY_KW)));
            diagnosticHelperLabel.setText("Run a demo diagnostic to simulate a panel alert state.");
            setStatus("Status: Not Generating", "status-pill-idle");
            return;
        }

        updateNotConfiguredState();
    }

    private void updateNotConfiguredState() {
        generationProgressBar.setProgress(0.0);
        solarGenerationLabel.setText("Solar not configured");
        solarCapacityLabel.setText("Add solar capacity to enable live output details.");
        diagnosticHelperLabel.setText("This home does not currently have solar configured.");
        setStatus("Status: Solar not configured", "status-pill-disabled");
    }

    private void updateDiagnosticButtonState() {
        boolean solarConfigured = homeEnergyAssets.hasSolar();
        simulateFaultButton.setDisable(!solarConfigured);
        simulateFaultButton.setVisible(solarConfigured);
        simulateFaultButton.setManaged(solarConfigured);
        simulateFaultButton.setText(isSolarFaultActive ? "Clear diagnostic alert" : "Run panel diagnostic");
    }

    private void setStatus(String text, String modifierStyleClass) {
        solarStatusLabel.setText(text);

        List<String> classesToRemove = new ArrayList<>(solarStatusLabel.getStyleClass());
        classesToRemove.remove("label");
        solarStatusLabel.getStyleClass().removeAll(classesToRemove);
        solarStatusLabel.getStyleClass().addAll(BASE_STATUS_STYLE, modifierStyleClass);
    }

    static double solarKwFromHalfHourKwh(double solarGenKwh) {
        return Math.max(solarGenKwh * 2.0, 0.0);
    }

    static double capacityProgress(double solarKw, double solarCapacityKw) {
        return solarCapacityKw <= 0.0 ? 0.0 : clamp(solarKw / solarCapacityKw, 0.0, 1.0);
    }

    static String statusStyleClassFor(String solarStatus) {
        return switch (solarStatus) {
            case "Status: Optimal" -> "status-pill-optimal";
            case "Status: Underperforming" -> "status-pill-underperforming";
            case "Status: Not Generating" -> "status-pill-idle";
            default -> "status-pill-idle";
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
