package com.energyapp.utils;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.Dishwasher;
import com.energyapp.model.EVCharger;
import com.energyapp.model.Fridge;
import com.energyapp.model.HeatingDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;

public final class DeviceFactory {

    private DeviceFactory() {}

    public static Device createDevice(String name, DeviceType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case SMART_PLUG -> new SmartPlug(name);
            case FRIDGE -> new Fridge(name);
            case WASHING_MACHINE -> new WashingMachine(name);
            case HEATING -> new HeatingDevice(name);
            case DISHWASHER -> new Dishwasher(name);
            case EV_CHARGER -> new EVCharger(name);
        };
    }

}
