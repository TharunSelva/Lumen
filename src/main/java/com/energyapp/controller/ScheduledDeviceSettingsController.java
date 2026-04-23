package com.energyapp.controller;

import com.energyapp.model.ScheduledDevice;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ScheduledDeviceSettingsController {

    @FXML private Button closeDeviceViewbtn;
    @FXML private Button deleteDevice;
    @FXML private Label deviceStatusLabel;
    @FXML private BorderPane deviceView;
    @FXML private Label deviceViewTitle;
    @FXML private Pane deviewViewMainPane;
    @FXML private Label estimateLabel;
    @FXML private Label estimateText;
    @FXML private Button forceRun;
    @FXML private CheckBox greenEnergy;
    @FXML private GridPane gridForDeviceView;
    @FXML private ImageView imageForCloseDevice;
    @FXML private ImageView indicatorIcon;
    @FXML private Pane pane1;
    @FXML private Pane pane2;
    @FXML private Pane pane3;
    @FXML private Pane pane4;
    @FXML private ImageView powerIcon;
    @FXML private Label powerLabel;
    @FXML private ToggleButton powerToggle;
    @FXML private Label powerText;
    @FXML private Label powerText1;
    @FXML private Label powerText11;
    @FXML private ImageView settingsIcon;
    @FXML private Label settingsText;
    @FXML private Label statusLabel;
    @FXML private Label statusText;
    @FXML private TextField scheduledTime;
    @FXML private Button scheduleUpdate;

    private ScheduledDevice scheduledDevice;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    public void initData(ScheduledDevice scheduledDevice) {
        this.scheduledDevice = scheduledDevice;
        deviceViewTitle.setText(this.scheduledDevice.getDeviceName());
        if (this.scheduledDevice.getSchedule() != null) {
            scheduledTime.setText(this.scheduledDevice.getSchedule().format(formatter));
        }
    }

    @FXML
    public void updateSchedule(){
        String input = scheduledTime.getText() == null ? "" : scheduledTime.getText().trim();

        if (input.isEmpty()) {
            showInvalidTimeAlert("Scheduled time cannot be empty. Please use HH:mm format.");
            return;
        }

        try {
            LocalTime schedule = LocalTime.parse(input, formatter);
            scheduledDevice.setSchedule(schedule);
            scheduledTime.setText(schedule.format(formatter));
        } catch (DateTimeParseException ex) {
            showInvalidTimeAlert("Invalid time. Please enter time as HH:mm, for example 07:30 or 18:45.");
        }
    }

    private void showInvalidTimeAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Time");
        alert.setHeaderText("Could not update schedule");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void skipSchedule(){
        scheduledDevice.setSchedule(null);
        scheduledTime.setText("");

    }

}
