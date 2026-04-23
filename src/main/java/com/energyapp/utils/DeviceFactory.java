package com.energyapp.utils;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;

public final class DeviceFactory {

    private DeviceFactory() {}

    public static Device createDevice(String name, DeviceType type) {
        Device newDevice;
        if (type == DeviceType.SMART_PLUG) {
            newDevice = new SmartPlug(name, type);
        } else if (type == DeviceType.WASHING_MACHINE) {
            newDevice = new ScheduledDevice(name, type);
        } else {
            return null;
            // add logic here to add the other device types (washing machine, fridge, etc...) later
        }

        return newDevice;
    }

}
