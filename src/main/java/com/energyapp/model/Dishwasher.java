package com.energyapp.model;

public final class Dishwasher extends ScheduledDevice {

    public Dishwasher(String deviceName) {
        super(deviceName, DeviceType.DISHWASHER);
        setOn(false);
        setStatus(DeviceStatus.OFF);
    }
}
