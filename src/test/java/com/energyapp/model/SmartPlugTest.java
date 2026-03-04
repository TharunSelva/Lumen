package com.energyapp.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmartPlugTest {

    @Test
    public void testSmartPlugInitialization() {
        SmartPlug plug = new SmartPlug("Desk Lamp", DeviceType.SMART_PLUG);
        assertEquals("Desk Lamp", plug.getDeviceName());
        assertEquals(DeviceType.SMART_PLUG, plug.getDeviceType());
        assertTrue(plug.isOn());
        assertEquals(20, plug.getPriceCutoff());
    }

    @Test
    public void testSetPriceCutoff() {
        SmartPlug plug = new SmartPlug("TV", DeviceType.SMART_PLUG);
        plug.setPriceCutoff(0.25);
        assertEquals(0.25, plug.getPriceCutoff());
    }
}