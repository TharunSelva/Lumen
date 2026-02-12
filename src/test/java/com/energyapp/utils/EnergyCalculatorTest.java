package com.energyapp.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnergyCalculatorTest {

    @Test
    void testCalculateCost_Normal() {
        assertEquals(50.0, EnergyCalculator.calculateCostInPounds(100.0, 50.0), 0.001);
    }

    @Test
    void testCalculateCost_NegativeIsZero() {
        assertEquals(0, EnergyCalculator.calculateCostInPounds(-50.0, 10.0));
    }

    @Test
    void testUsageLevel_Low() {
        assertEquals("Low Usage", EnergyCalculator.determineEnergyUsageLevel(0.04));
    }

    @Test
    void testUsageLevel_Standard() {
        assertEquals("Standard Usage", EnergyCalculator.determineEnergyUsageLevel(0.10));
    }

    @Test
    void testUsageLevel_High() {
        assertEquals("High Usage", EnergyCalculator.determineEnergyUsageLevel(0.15));
        assertEquals("High Usage", EnergyCalculator.determineEnergyUsageLevel(0.50));
    }

    @Test
    void testSolarBreakdown_SunnyDay() {
        double load = 10.0;
        double solar = 20.0;

        assertEquals(10.0, EnergyCalculator.calculateSolarUsed(load, solar));
    }

    @Test
    void testSolarBreakdown_CloudyDay() {
        double load = 10.0;
        double solar = 4.0;

        assertEquals(4.0, EnergyCalculator.calculateSolarUsed(load, solar));
    }

    @Test
    void testBatteryUsage_NeedsBattery() {
        double load = 10.0;
        double solar = 0.0;
        double grid = 2.0;

        assertEquals(8.0, EnergyCalculator.calculateBatteryUsed(load, solar, grid));
    }

    @Test
    void testCo2Labels() {
        assertEquals("Low CO2", EnergyCalculator.determineCo2Label(100));
        assertEquals("Medium CO2", EnergyCalculator.determineCo2Label(200));
        assertEquals("High CO2", EnergyCalculator.determineCo2Label(300));
    }
}
