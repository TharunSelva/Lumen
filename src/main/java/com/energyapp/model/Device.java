package com.energyapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public abstract class Device {

    private Long persistedId;
    private String deviceName;
    private DeviceType deviceType;

    // allows the ui to listen dynamically to changes, which isn't possible with a regular Java boolean.
    private BooleanProperty isOn = new SimpleBooleanProperty(true);

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

}
