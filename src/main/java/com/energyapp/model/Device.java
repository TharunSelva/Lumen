package com.energyapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDateTime;

public abstract class Device {

    private String deviceId;
    private Long persistedId;
    private final String deviceName;
    private final DeviceType deviceType;

    // allows the ui to listen dynamically to changes, which isn't possible with a regular Java boolean.
    private final BooleanProperty isOn = new SimpleBooleanProperty(true);
    private final ObjectProperty<DeviceStatus> status = new SimpleObjectProperty<>(DeviceStatus.IDLE);
    private final BooleanProperty greenEnergyOnly = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> activeRunUntil = new SimpleObjectProperty<>();
    private final ObjectProperty<DeviceRunMode> runMode = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastRunStartedAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastRunEndedAt = new SimpleObjectProperty<>();

    public Device(String deviceName, DeviceType deviceType) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public Long getPersistedId() {
        return persistedId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setPersistedId(Long persistedId) {
        this.persistedId = persistedId;
    }

    public DeviceType getDeviceType() {
        return this.deviceType;
    }

    public boolean isOn() {
        return isOn.get();
    }

    public void setOn(boolean val) {
        isOn.set(val);
    }

    // method that allows ui to listen to changes in the power status of a device
    public BooleanProperty isOnProperty() {
        return this.isOn;
    }

    public DeviceStatus getStatus() {
        return status.get();
    }

    public void setStatus(DeviceStatus value) {
        status.set(value);
    }

    public ObjectProperty<DeviceStatus> statusProperty() {
        return status;
    }

    public boolean isGreenEnergyOnly() {
        return greenEnergyOnly.get();
    }

    public void setGreenEnergyOnly(boolean value) {
        greenEnergyOnly.set(value);
    }

    public BooleanProperty greenEnergyOnlyProperty() {
        return greenEnergyOnly;
    }

    public LocalDateTime getActiveRunUntil() {
        return activeRunUntil.get();
    }

    public void setActiveRunUntil(LocalDateTime value) {
        activeRunUntil.set(value);
    }

    public ObjectProperty<LocalDateTime> activeRunUntilProperty() {
        return activeRunUntil;
    }

    public DeviceRunMode getRunMode() {
        return runMode.get();
    }

    public void setRunMode(DeviceRunMode value) {
        runMode.set(value);
    }

    public ObjectProperty<DeviceRunMode> runModeProperty() {
        return runMode;
    }

    public LocalDateTime getLastRunStartedAt() {
        return lastRunStartedAt.get();
    }

    public void setLastRunStartedAt(LocalDateTime value) {
        lastRunStartedAt.set(value);
    }

    public ObjectProperty<LocalDateTime> lastRunStartedAtProperty() {
        return lastRunStartedAt;
    }

    public LocalDateTime getLastRunEndedAt() {
        return lastRunEndedAt.get();
    }

    public void setLastRunEndedAt(LocalDateTime value) {
        lastRunEndedAt.set(value);
    }

    public ObjectProperty<LocalDateTime> lastRunEndedAtProperty() {
        return lastRunEndedAt;
    }

}
