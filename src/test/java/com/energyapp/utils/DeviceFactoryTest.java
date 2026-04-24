package com.energyapp.utils;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.Dishwasher;
import com.energyapp.model.EVCharger;
import com.energyapp.model.Fridge;
import com.energyapp.model.HeatingDevice;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceFactoryTest {

    @Test
    void createDevice_returnsConcreteSubclassForEachSupportedType() {
        assertInstanceOf(SmartPlug.class, DeviceFactory.createDevice("Desk Lamp", DeviceType.SMART_PLUG));
        assertInstanceOf(Fridge.class, DeviceFactory.createDevice("Kitchen Fridge", DeviceType.FRIDGE));
        assertInstanceOf(WashingMachine.class, DeviceFactory.createDevice("Laundry", DeviceType.WASHING_MACHINE));
        assertInstanceOf(HeatingDevice.class, DeviceFactory.createDevice("Boiler", DeviceType.HEATING));
        assertInstanceOf(Dishwasher.class, DeviceFactory.createDevice("Dishwasher", DeviceType.DISHWASHER));
        assertInstanceOf(EVCharger.class, DeviceFactory.createDevice("Driveway EV", DeviceType.EV_CHARGER));
    }

    @Test
    void createDevice_marksScheduledTypesWithScheduledHierarchy() {
        Device washingMachine = DeviceFactory.createDevice("Laundry", DeviceType.WASHING_MACHINE);
        Device dishwasher = DeviceFactory.createDevice("Dishes", DeviceType.DISHWASHER);
        Device evCharger = DeviceFactory.createDevice("EV", DeviceType.EV_CHARGER);
        Device fridge = DeviceFactory.createDevice("Kitchen Fridge", DeviceType.FRIDGE);

        assertNotNull(washingMachine);
        assertNotNull(dishwasher);
        assertNotNull(evCharger);
        assertTrue(washingMachine instanceof ScheduledDevice);
        assertTrue(dishwasher instanceof ScheduledDevice);
        assertTrue(evCharger instanceof ScheduledDevice);
        assertTrue(!(fridge instanceof ScheduledDevice));
    }

    @Test
    void createDevice_returnsNullWhenTypeMissing() {
        assertNull(DeviceFactory.createDevice("Unknown", null));
    }
}
