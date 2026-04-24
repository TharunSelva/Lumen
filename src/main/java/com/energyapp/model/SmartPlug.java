package com.energyapp.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SmartPlug extends Device {

    private final DoubleProperty priceCutoff = new SimpleDoubleProperty(20);

    public SmartPlug(String deviceName) {
        this(deviceName, DeviceType.SMART_PLUG);
    }

    public SmartPlug(String deviceName, DeviceType deviceType) {
        super(deviceName, deviceType);
        setStatus(DeviceStatus.IDLE);
    }

    public double getPriceCutoff() {
        return this.priceCutoff.get();
    }

    public void setPriceCutoff(double val) {
        priceCutoff.set(val);
    }

    public DoubleProperty priceCutoffProperty() {
        return this.priceCutoff;
    }
}
