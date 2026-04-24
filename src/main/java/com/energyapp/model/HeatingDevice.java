package com.energyapp.model;

public final class HeatingDevice extends Device {

    public HeatingDevice(String deviceName) {
        super(deviceName, DeviceType.HEATING);
        setOn(false);
        setStatus(DeviceStatus.OFF);
    }
}
