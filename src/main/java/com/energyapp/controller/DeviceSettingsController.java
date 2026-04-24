package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.service.DeviceService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DeviceSettingsController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "OFF";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusChipLabel;
    @FXML private Label currentStateLabel;
    @FXML private ToggleButton powerToggle;
    @FXML private CheckBox greenEnergyToggle;
    @FXML private VBox scheduleRow;
    @FXML private CheckBox scheduleEnabledToggle;
    @FXML private TextField scheduleTimeField;
    @FXML private Label scheduleHelperLabel;
    @FXML private HBox smartPlugRow;
    @FXML private TextField priceCutoffField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;

    private DeviceService deviceService;
    private Device device;
    private String displayNameOverride;
    private boolean deleted;

    @FXML
    public void initialize() {
        powerToggle.textProperty().bind(Bindings.when(powerToggle.selectedProperty())
                .then("On")
                .otherwise("Off"));
        scheduleTimeField.setPrefWidth(180.0);
        scheduleTimeField.setMaxWidth(180.0);
        if (!scheduleTimeField.getStyleClass().contains("devices-time-field")) {
            scheduleTimeField.getStyleClass().add("devices-time-field");
        }
        if (!scheduleHelperLabel.getStyleClass().contains("devices-settings-helper")) {
            scheduleHelperLabel.getStyleClass().add("devices-settings-helper");
        }
        cancelButton.setOnAction(event -> closeWindow());
        scheduleEnabledToggle.selectedProperty().addListener((obs, oldValue, newValue) -> updateScheduleFieldState());
    }

    public void init(DeviceService deviceService, Device device) {
        init(deviceService, device, device.getDeviceName());
    }

    public void init(DeviceService deviceService, Device device, String displayNameOverride) {
        this.deviceService = deviceService;
        this.device = device;
        this.displayNameOverride = displayNameOverride;

        titleLabel.setText(displayNameOverride == null || displayNameOverride.isBlank() ? device.getDeviceName() : displayNameOverride);
        subtitleLabel.setText(formatDeviceType(device));
        powerToggle.setSelected(device.isOn());
        greenEnergyToggle.setSelected(device.isGreenEnergyOnly());
        syncStatusChip();
        currentStateLabel.setText(buildCurrentState(device));

        boolean scheduled = device instanceof ScheduledDevice;
        scheduleRow.setManaged(scheduled);
        scheduleRow.setVisible(scheduled);

        boolean smartPlug = device instanceof SmartPlug;
        smartPlugRow.setManaged(smartPlug);
        smartPlugRow.setVisible(smartPlug);

        if (scheduled) {
            ScheduledDevice scheduledDevice = (ScheduledDevice) device;
            boolean scheduleEnabled = scheduledDevice.getSchedule() != null;
            scheduleEnabledToggle.setSelected(scheduleEnabled);
            scheduleTimeField.setText(scheduleEnabled ? scheduledDevice.getSchedule().format(TIME_FMT) : "");
            updateScheduleFieldState();
            updateScheduleHelperText();
        }

        if (smartPlug) {
            priceCutoffField.setText(String.format("%.1f", ((SmartPlug) device).getPriceCutoff()));
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    @FXML
    void handleSave() {
        clearError();

        if (device instanceof SmartPlug smartPlug) {
            Double cutoff = parsePriceCutoffField();
            if (cutoff == null) {
                return;
            }
            smartPlug.setPriceCutoff(cutoff);
        }

        if (device instanceof ScheduledDevice scheduledDevice) {
            if (scheduleEnabledToggle.isSelected()) {
                Optional<LocalTime> newTime = parseScheduleField();
                if (newTime.isEmpty()) {
                    return;
                }
                scheduledDevice.setSchedule(newTime.get());
                scheduledDevice.setSkipNextRun(false);
                if (!scheduledDevice.isOn()) {
                    scheduledDevice.setStatus(DeviceStatus.SCHEDULED);
                }
            } else {
                scheduledDevice.setSchedule(null);
                scheduledDevice.setSkipNextRun(false);
                if (!scheduledDevice.isOn()) {
                    scheduledDevice.setStatus(DeviceStatus.OFF);
                }
            }
        }

        device.setGreenEnergyOnly(greenEnergyToggle.isSelected());

        boolean attemptedLockedPowerOff = !powerToggle.isSelected() && deviceService.isPowerLocked(device);
        applyPowerState();
        deviceService.saveDevice(device);

        if (attemptedLockedPowerOff) {
            syncFormFromDevice();
            return;
        }

        closeWindow();
    }

    @FXML
    void handleDelete() {
        clearError();
        if (!confirmDelete()) {
            return;
        }

        deviceService.deleteDevice(device);
        deleted = true;
        closeWindow();
    }

    boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete device");
        alert.setHeaderText("Remove " + titleLabel.getText() + "?");
        alert.setContentText("This keeps historical usage data, but removes the device from the Devices page.");
        return alert.showAndWait().filter(buttonType -> buttonType == javafx.scene.control.ButtonType.OK).isPresent();
    }

    private void applyPowerState() {
        boolean shouldBeOn = powerToggle.isSelected();
        if (!shouldBeOn && deviceService.isPowerLocked(device)) {
            powerToggle.setSelected(true);
            device.setOn(true);
            device.setStatus(DeviceStatus.RUNNING);
            scheduleHelperLabel.setText("This washing cycle is already running and cannot be turned off until it finishes.");
            return;
        }

        device.setOn(shouldBeOn);
        if (!shouldBeOn) {
            device.setActiveRunUntil(null);
            device.setRunMode(null);
            device.setStatus(device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getSchedule() != null
                    ? DeviceStatus.SCHEDULED
                    : DeviceStatus.OFF);
            updateScheduleHelperText();
            return;
        }

        if (device.getActiveRunUntil() != null) {
            device.setStatus(DeviceStatus.RUNNING);
        } else {
            device.setStatus(DeviceStatus.IDLE);
        }
        updateScheduleHelperText();
    }

    private void syncFormFromDevice() {
        powerToggle.setSelected(device.isOn());
        greenEnergyToggle.setSelected(device.isGreenEnergyOnly());
        syncStatusChip();
        currentStateLabel.setText(buildCurrentState(device));
        if (device instanceof ScheduledDevice scheduledDevice) {
            boolean scheduleEnabled = scheduledDevice.getSchedule() != null;
            scheduleEnabledToggle.setSelected(scheduleEnabled);
            scheduleTimeField.setText(scheduleEnabled ? scheduledDevice.getSchedule().format(TIME_FMT) : "");
            updateScheduleFieldState();
            updateScheduleHelperText();
        }
    }

    private void syncStatusChip() {
        String statusText = deviceService == null ? formatStatus(device.getStatus()) : deviceService.presentationStatus(device);
        statusChipLabel.setText(statusText);
        statusChipLabel.getStyleClass().setAll("device-status-chip", statusClass(statusText));
    }

    private void updateScheduleFieldState() {
        boolean enabled = scheduleEnabledToggle.isSelected();
        scheduleTimeField.setDisable(!enabled);
        if (!enabled) {
            scheduleTimeField.clear();
            if (!scheduleTimeField.getStyleClass().contains("devices-disabled-field")) {
                scheduleTimeField.getStyleClass().add("devices-disabled-field");
            }
        } else {
            scheduleTimeField.getStyleClass().remove("devices-disabled-field");
        }
        updateScheduleHelperText();
    }

    private void updateScheduleHelperText() {
        if (deviceService != null && deviceService.isPowerLocked(device)) {
            scheduleHelperLabel.setText("This washing cycle is already running and cannot be turned off until it finishes.");
            return;
        }
        if (!(device instanceof ScheduledDevice scheduledDevice)) {
            scheduleHelperLabel.setText("");
            return;
        }
        if (!scheduleEnabledToggle.isSelected()) {
            scheduleHelperLabel.setText("Turn on scheduling to choose a saved run time.");
            return;
        }
        if (scheduledDevice.getSchedule() != null) {
            scheduleHelperLabel.setText("This device is set to run at " + scheduledDevice.getSchedule().format(TIME_FMT) + ".");
            return;
        }
        scheduleHelperLabel.setText("Use HH:mm format to set the next automatic run.");
    }

    private Optional<LocalTime> parseScheduleField() {
        String rawText = scheduleTimeField.getText() == null ? "" : scheduleTimeField.getText().trim();
        if (rawText.isBlank()) {
            showError("Enter a time as HH:mm to save the schedule.");
            return Optional.empty();
        }

        try {
            LocalTime parsedTime = LocalTime.parse(rawText, TIME_FMT);
            scheduleTimeField.setText(parsedTime.format(TIME_FMT));
            return Optional.of(parsedTime);
        } catch (DateTimeParseException ex) {
            showError("Time must use HH:mm format, for example 07:30 or 22:15.");
            return Optional.empty();
        }
    }

    private Double parsePriceCutoffField() {
        String rawText = priceCutoffField.getText() == null ? "" : priceCutoffField.getText().trim();
        if (rawText.isBlank()) {
            showError("Enter a price cut-off for the smart plug.");
            return null;
        }

        try {
            double cutoff = Double.parseDouble(rawText);
            if (cutoff < 0) {
                showError("Price cut-off must be zero or higher.");
                return null;
            }
            priceCutoffField.setText(String.format("%.1f", cutoff));
            return cutoff;
        } catch (NumberFormatException ex) {
            showError("Price cut-off must be a number, for example 18.5.");
            return null;
        }
    }

    private String buildCurrentState(Device device) {
        if (device.getActiveRunUntil() != null && device.isOn()) {
            return "On until " + device.getActiveRunUntil().format(TIME_FMT);
        }
        if (device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getSchedule() != null) {
            return "Scheduled for " + scheduledDevice.getSchedule().format(TIME_FMT);
        }
        return device.isOn() ? "Currently on" : "Currently off";
    }

    private String formatDeviceType(Device device) {
        return device.getDeviceType().name().replace('_', ' ');
    }

    private String formatStatus(DeviceStatus status) {
        return status == null ? STATUS_OFF : status.name().replace('_', ' ');
    }

    private String statusClass(String status) {
        return switch (status) {
            case STATUS_ON -> "device-status-on";
            case STATUS_SCHEDULED -> "device-status-scheduled";
            case STATUS_SKIPPED -> "device-status-skipped";
            default -> "device-status-off";
        };
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void closeWindow() {
        if (cancelButton.getScene() != null && cancelButton.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }
}
