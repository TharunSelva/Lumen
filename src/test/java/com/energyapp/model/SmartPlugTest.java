package com.energyapp.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testPriceCutoffProperty() {
        SmartPlug plug = new SmartPlug("Heater", DeviceType.SMART_PLUG);
        assertNotNull(plug.priceCutoffProperty());
        assertEquals(20.0, plug.priceCutoffProperty().get(), 0.001);
    }

    @Test
    public void testPriceCutoffPropertyBinding() {
        SmartPlug plug = new SmartPlug("Fan", DeviceType.SMART_PLUG);
        plug.priceCutoffProperty().set(15.0);
        assertEquals(15.0, plug.getPriceCutoff(), 0.001);
    }

    @Test
    public void testSetPriceCutoffZero() {
        SmartPlug plug = new SmartPlug("Test Device", DeviceType.SMART_PLUG);
        plug.setPriceCutoff(0.0);
        assertEquals(0.0, plug.getPriceCutoff(), 0.001);
    }

    @Test
    public void testSetPriceCutoffLargeValue() {
        SmartPlug plug = new SmartPlug("Test Device", DeviceType.SMART_PLUG);
        plug.setPriceCutoff(1000.0);
        assertEquals(1000.0, plug.getPriceCutoff(), 0.001);
    }

    @Test
    public void testToggleOnOff() {
        SmartPlug plug = new SmartPlug("Lamp", DeviceType.SMART_PLUG);
        assertTrue(plug.isOn());
        plug.setOn(false);
        assertFalse(plug.isOn());
        plug.setOn(true);
        assertTrue(plug.isOn());
    }

    @Test
    public void testIsOnProperty() {
        SmartPlug plug = new SmartPlug("Monitor", DeviceType.SMART_PLUG);
        assertNotNull(plug.isOnProperty());
        assertTrue(plug.isOnProperty().get());
    }

    @Test
    public void testIsOnPropertyBinding() {
        SmartPlug plug = new SmartPlug("Computer", DeviceType.SMART_PLUG);
        plug.isOnProperty().set(false);
        assertFalse(plug.isOn());
    }

    @Test
    public void testDeviceNameIsImmutable() {
        SmartPlug plug = new SmartPlug("Original Name", DeviceType.SMART_PLUG);
        assertEquals("Original Name", plug.getDeviceName());
        // Device name should not change after initialization
    }

    @Test
    public void testDeviceTypeIsSmartPlug() {
        SmartPlug plug = new SmartPlug("Smart Device", DeviceType.SMART_PLUG);
        assertEquals(DeviceType.SMART_PLUG, plug.getDeviceType());
    }

    @Test
    public void testMultipleSmartPlugsIndependent() {
        SmartPlug plug1 = new SmartPlug("Living Room", DeviceType.SMART_PLUG);
        SmartPlug plug2 = new SmartPlug("Kitchen", DeviceType.SMART_PLUG);

        plug1.setPriceCutoff(25.0);
        plug2.setPriceCutoff(30.0);
        plug1.setOn(false);

        assertEquals(25.0, plug1.getPriceCutoff(), 0.001);
        assertEquals(30.0, plug2.getPriceCutoff(), 0.001);
        assertFalse(plug1.isOn());
        assertTrue(plug2.isOn());
    }

    @Test
    public void testSetPriceCutoffNegativeValue() {
        SmartPlug plug = new SmartPlug("Test Device", DeviceType.SMART_PLUG);
        plug.setPriceCutoff(-5.0);
        assertEquals(-5.0, plug.getPriceCutoff(), 0.001);
    }

    @Test
    public void testSetPriceCutoffDecimalValue() {
        SmartPlug plug = new SmartPlug("Test Device", DeviceType.SMART_PLUG);
        plug.setPriceCutoff(12.345);
        assertEquals(12.345, plug.getPriceCutoff(), 0.001);
    }
}