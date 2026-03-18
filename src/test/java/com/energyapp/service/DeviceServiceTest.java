package com.energyapp.service;

import com.energyapp.model.DeviceType;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.SmartPlug;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DeviceServiceTest {

    @Test
    public void testCutoffRuleTriggeredByPriceSpike() {
        SimulationService simService = new SimulationService();
        DeviceService deviceService = new DeviceService(simService);

        SmartPlug testPlug = new SmartPlug("Heater", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(0.30);
        testPlug.setOn(true);

        deviceService.getAllDevices().add(testPlug);
        EnergyRecord spike = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 80.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(spike);

        assertFalse(testPlug.isOn(), "Plug should turn off when price exceeds cutoff");
    }

}