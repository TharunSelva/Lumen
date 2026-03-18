package com.energyapp.controller;

import com.energyapp.model.ScheduledDevice;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public void initialize(ScheduledDevice scheduledDevice) {
        this.scheduledDevice = scheduledDevice;
        deviceViewTitle.setText(this.scheduledDevice.getDeviceName());
        if (this.scheduledDevice.getSchedule() != null) {
            scheduledTime.setText(this.scheduledDevice.getSchedule().format(formatter));
        }
    }

    @FXML
    public void updateSchedule(){
        LocalDateTime schedule = LocalDateTime.parse(scheduledTime.getText(), formatter);
        scheduledDevice.setSchedule(schedule);
    }

    @FXML
    public void skipSchedule(){
        scheduledDevice.setSchedule(null);
        scheduledTime.setText("");

    }

}
