package com.energyapp.model;

public final class Fridge extends Device {

    public Fridge(String deviceName) {
        super(deviceName, DeviceType.FRIDGE);
        setOn(true);
        setStatus(DeviceStatus.IDLE);
    }
}
