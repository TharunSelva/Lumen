package com.energyapp.service;

import com.energyapp.model.DeviceType;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class DeviceServiceTest {

    private SimulationService simService;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        simService = new SimulationService();
        deviceService = new DeviceService(simService);
    }

    @Test
    public void testCutoffRuleTriggeredByPriceSpike() {
        SmartPlug testPlug = new SmartPlug("Heater", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(0.30);
        testPlug.setOn(true);

        deviceService.getAllDevices().add(testPlug);
        EnergyRecord spike = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 80.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(spike);

        assertFalse(testPlug.isOn(), "Plug should turn off when price exceeds cutoff");
    }

    @Test
    public void testCutoffRuleNotTriggeredWhenPriceBelowCutoff() {
        SmartPlug testPlug = new SmartPlug("Fan", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(50.0);
        testPlug.setOn(true);

        deviceService.getAllDevices().add(testPlug);
        EnergyRecord lowPrice = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 30.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(lowPrice);

        assertTrue(testPlug.isOn(), "Plug should remain on when price is below cutoff");
    }

    @Test
    public void testCutoffRuleNotTriggeredWhenDeviceAlreadyOff() {
        SmartPlug testPlug = new SmartPlug("Lamp", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(10.0);
        testPlug.setOn(false);

        deviceService.getAllDevices().add(testPlug);
        EnergyRecord spike = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 80.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(spike);

        assertFalse(testPlug.isOn(), "Plug should remain off");
    }

    @Test
    public void testMultipleDevicesDifferentCutoffs() {
        SmartPlug plug1 = new SmartPlug("Device 1", DeviceType.SMART_PLUG);
        plug1.setPriceCutoff(25.0);
        plug1.setOn(true);

        SmartPlug plug2 = new SmartPlug("Device 2", DeviceType.SMART_PLUG);
        plug2.setPriceCutoff(75.0);
        plug2.setOn(true);

        deviceService.addDevice(plug1);
        deviceService.addDevice(plug2);

        EnergyRecord priceSpike = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 50.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(priceSpike);

        assertFalse(plug1.isOn(), "Device 1 should turn off (price 50 > cutoff 25)");
        assertTrue(plug2.isOn(), "Device 2 should stay on (price 50 < cutoff 75)");
    }

    @Test
    public void testCutoffAtExactThreshold() {
        SmartPlug testPlug = new SmartPlug("Exact Threshold", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(50.0);
        testPlug.setOn(true);

        deviceService.addDevice(testPlug);
        EnergyRecord exactPrice = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 50.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(exactPrice);

        assertTrue(testPlug.isOn(), "Plug should stay on when price equals cutoff (not exceeded)");
    }

    @Test
    public void testGetAllDevicesReturnsCorrectList() {
        assertNotNull(deviceService.getAllDevices());
        for (Device device : deviceService.getAllDevices()) {
            assertEquals(DeviceType.SMART_PLUG, device.getDeviceType());
        }
    }

    @Test
    public void testZeroPriceCutoff() {
        SmartPlug testPlug = new SmartPlug("Zero Cutoff", DeviceType.SMART_PLUG);
        testPlug.setPriceCutoff(0.0);
        testPlug.setOn(true);

        deviceService.addDevice(testPlug);
        EnergyRecord anyPrice = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 10.0, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(anyPrice);

        assertFalse(testPlug.isOn(), "Plug with zero cutoff should turn off at any price > 0");
    }

}