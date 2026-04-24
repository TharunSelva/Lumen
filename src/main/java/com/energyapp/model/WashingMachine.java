package com.energyapp.model;

public final class WashingMachine extends ScheduledDevice {

    public WashingMachine(String deviceName) {
        super(deviceName, DeviceType.WASHING_MACHINE);
        setOn(false);
        setStatus(DeviceStatus.OFF);
    }
}
