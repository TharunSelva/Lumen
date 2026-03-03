package com.energyapp.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SmartPlug extends Device {

    private DoubleProperty priceCutoff = new SimpleDoubleProperty(0.50);

    public SmartPlug(String deviceName, DeviceType deviceType) {
        super(deviceName, deviceType);
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
