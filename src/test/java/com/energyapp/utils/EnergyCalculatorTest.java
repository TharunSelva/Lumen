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

    @Test
    void testCalculatePercentage_Normal() {
        assertEquals(50, EnergyCalculator.calculatePercentage(50.0, 100.0));
        assertEquals(25, EnergyCalculator.calculatePercentage(25.0, 100.0));
    }

    @Test
    void testCalculatePercentage_rounding() {
        assertEquals(33, EnergyCalculator.calculatePercentage(1.0, 3.0));
        assertEquals(67, EnergyCalculator.calculatePercentage(2.0, 3.0));
    }

    @Test
    void testCalculatePercentage_ZeroTotal() {
        assertEquals(0, EnergyCalculator.calculatePercentage(50.0, 0.0));
    }

    @Test
    void testCalculateBatteryUsed_MixedSources() {
        double totalLoad = 10.0;
        double solarGen = 3.0;
        double gridImport = 4.0;

        double result = EnergyCalculator.calculateBatteryUsed(totalLoad, solarGen, gridImport);

        assertEquals(3.0, result, 0.001, "Battery should cover the load not met by Solar or Grid");
    }

    @Test
    void testCalculateBatteryUsed_SurplusSolar() {
        double result = EnergyCalculator.calculateBatteryUsed(5.0, 10.0, 0.0);
        assertEquals(0.0, result, 0.001);
    }


@Test
void testCalculateCleanShare_Normal() {
    // 5.0 solar out of 10.0 total load should be 50.0%
    assertEquals(50.0, EnergyCalculator.calculateCleanShare(10.0, 5.0), 0.001);
}

@Test
void testCalculateCleanShare_ZeroTotalLoad() {
    // If total load is 0, it should safely return 0.0 without dividing by zero
    assertEquals(0.0, EnergyCalculator.calculateCleanShare(0.0, 0.0), 0.001);
}

@Test
void testCalculateCleanShare_AllSolar() {
    // If solar matches total load exactly, it should be 100%
    assertEquals(100.0, EnergyCalculator.calculateCleanShare(12.5, 12.5), 0.001);
}

@Test
void testCalculateCleanShare_NoSolar() {
    // If there is load but no solar, it should be 0%
    assertEquals(0.0, EnergyCalculator.calculateCleanShare(10.0, 0.0), 0.001);
}
}
