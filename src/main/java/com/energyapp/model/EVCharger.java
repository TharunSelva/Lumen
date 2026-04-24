package com.energyapp.model;

public final class EVCharger extends ScheduledDevice {

    public EVCharger(String deviceName) {
        super(deviceName, DeviceType.EV_CHARGER);
        setOn(false);
        setStatus(DeviceStatus.OFF);
    }
}
