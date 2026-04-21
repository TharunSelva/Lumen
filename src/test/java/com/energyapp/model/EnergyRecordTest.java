package com.energyapp.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class EnergyRecordTest {

    @Test
    void testConstructor_CalculatesDerivedFields() {
        LocalDateTime now = LocalDateTime.now();
        double loadKwh = 1.5;

        EnergyRecord record = new EnergyRecord(
                now, "Label", 0.0, 0.0, 0.0,
                loadKwh,
                0.0, 0, 0.0, 0.0, 0.0, 0.0
        );

        assertEquals(3000.0, record.getWatts(), 0.001, "Watts should be derived from Load Kwh * 2000");
    }

    @Test
    void testGetTimestamp() {
        LocalDateTime expectedTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
        EnergyRecord record = new EnergyRecord(expectedTime, "14:30", 5.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(expectedTime, record.getTimestamp());
    }

    @Test
    void testGetGridImport() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 7.5, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(7.5, record.getGridImport(), 0.001);
    }

    @Test
    void testGetPricePerKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 35.5, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(35.5, record.getPricePerKwh(), 0.001);
    }

    @Test
    void testGetCarbon() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 175.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(175.0, record.getCarbon(), 0.001);
    }

    @Test
    void testGetTotalLoadKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 12.5, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(12.5, record.getTotalLoadKwh(), 0.001);
    }

    @Test
    void testGetSolarGenKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 4.25, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(4.25, record.getSolarGenKwh(), 0.001);
    }

    @Test
    void testGetBatteryLevel() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 85, 2.0, 1.0, 1.5, 0.5);
        assertEquals(85, record.getBatteryLevel());
    }

    @Test
    void testGetEvChargerUsageKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 75, 3.5, 1.0, 1.5, 0.5);
        assertEquals(3.5, record.getEvChargerUsageKwh(), 0.001);
    }

    @Test
    void testGetWashingMachineUsageKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 2.5, 1.5, 0.5);
        assertEquals(2.5, record.getWashingMachineUsageKwh(), 0.001);
    }

    @Test
    void testGetHeatingUsageKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 3.0, 0.5);
        assertEquals(3.0, record.getHeatingUsageKwh(), 0.001);
    }

    @Test
    void testGetBaseloadKwh() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 1.25);
        assertEquals(1.25, record.getBaseloadKwh(), 0.001);
    }

    @Test
    void testCalculateEnergyCost() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 10.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        // cost = gridImport * pricePerKwh = 10.0 * 20.0 = 200.0
        assertEquals(200.0, record.calculateEnergyCost(), 0.001);
    }

    @Test
    void testCalculateEnergyCost_ZeroGridImport() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 0.0, 20.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(0.0, record.calculateEnergyCost(), 0.001);
    }

    @Test
    void testCalculateEnergyCost_ZeroPrice() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 10.0, 0.0, 100.0, 10.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(0.0, record.calculateEnergyCost(), 0.001);
    }

    @Test
    void testWatts_ZeroLoad() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 0.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(0.0, record.getWatts(), 0.001);
    }

    @Test
    void testWatts_HighLoad() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 50.0, 3.0, 75, 2.0, 1.0, 1.5, 0.5);
        assertEquals(100000.0, record.getWatts(), 0.001);
    }

    @Test
    void testBatteryLevel_Zero() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 0, 2.0, 1.0, 1.5, 0.5);
        assertEquals(0, record.getBatteryLevel());
    }

    @Test
    void testBatteryLevel_Full() {
        EnergyRecord record = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100.0, 10.0, 3.0, 100, 2.0, 1.0, 1.5, 0.5);
        assertEquals(100, record.getBatteryLevel());
    }
}