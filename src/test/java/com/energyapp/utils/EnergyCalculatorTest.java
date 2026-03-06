package com.energyapp.utils;

import com.energyapp.model.EnergyRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

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

    // Additional test cases

    @Test
    void testCalculateCost_Zero() {
        assertEquals(0.0, EnergyCalculator.calculateCostInPounds(0.0, 50.0), 0.001);
        assertEquals(0.0, EnergyCalculator.calculateCostInPounds(100.0, 0.0), 0.001);
    }

    @Test
    void testCalculateCost_NegativePrice() {
        assertEquals(0.0, EnergyCalculator.calculateCostInPounds(100.0, -10.0), 0.001);
    }

    @Test
    void testCalculateCost_SmallValues() {
        assertEquals(0.01, EnergyCalculator.calculateCostInPounds(1.0, 1.0), 0.001);
    }

    @Test
    void testCalculateCost_LargeValues() {
        assertEquals(5000.0, EnergyCalculator.calculateCostInPounds(1000.0, 500.0), 0.001);
    }

    @Test
    void testUsageLevel_Boundaries() {
        assertEquals("Low Usage", EnergyCalculator.determineEnergyUsageLevel(0.0));
        assertEquals("Low Usage", EnergyCalculator.determineEnergyUsageLevel(0.049));
        assertEquals("Standard Usage", EnergyCalculator.determineEnergyUsageLevel(0.05));
        assertEquals("Standard Usage", EnergyCalculator.determineEnergyUsageLevel(0.149));
        assertEquals("High Usage", EnergyCalculator.determineEnergyUsageLevel(0.15));
    }

    @Test
    void testSolarUsed_ZeroLoad() {
        assertEquals(0.0, EnergyCalculator.calculateSolarUsed(0.0, 10.0), 0.001);
    }

    @Test
    void testSolarUsed_ZeroSolar() {
        assertEquals(0.0, EnergyCalculator.calculateSolarUsed(10.0, 0.0), 0.001);
    }

    @Test
    void testSolarUsed_ExactMatch() {
        assertEquals(10.0, EnergyCalculator.calculateSolarUsed(10.0, 10.0), 0.001);
    }

    @Test
    void testBatteryUsed_ZeroLoad() {
        assertEquals(0.0, EnergyCalculator.calculateBatteryUsed(0.0, 5.0, 3.0), 0.001);
    }

    @Test
    void testBatteryUsed_AllFromGrid() {
        assertEquals(0.0, EnergyCalculator.calculateBatteryUsed(10.0, 0.0, 10.0), 0.001);
    }

    @Test
    void testBatteryUsed_AllFromSolar() {
        assertEquals(0.0, EnergyCalculator.calculateBatteryUsed(10.0, 10.0, 0.0), 0.001);
    }

    @Test
    void testBatteryUsed_ExactMix() {
        // 10 load, 5 solar, 5 grid = 0 from battery
        assertEquals(0.0, EnergyCalculator.calculateBatteryUsed(10.0, 5.0, 5.0), 0.001);
    }

    @Test
    void testCo2Label_Boundaries() {
        assertEquals("Low CO2", EnergyCalculator.determineCo2Label(0));
        assertEquals("Low CO2", EnergyCalculator.determineCo2Label(150));
        assertEquals("Medium CO2", EnergyCalculator.determineCo2Label(151));
        assertEquals("Medium CO2", EnergyCalculator.determineCo2Label(250));
        assertEquals("High CO2", EnergyCalculator.determineCo2Label(251));
    }

    @Test
    void testCalculatePercentage_100Percent() {
        assertEquals(100, EnergyCalculator.calculatePercentage(100.0, 100.0));
    }

    @Test
    void testCalculatePercentage_MoreThan100Percent() {
        assertEquals(200, EnergyCalculator.calculatePercentage(200.0, 100.0));
    }

    @Test
    void testCalculatePercentage_SmallValues() {
        assertEquals(50, EnergyCalculator.calculatePercentage(0.5, 1.0));
    }

    @Test
    void testDetermineSolarGenLevel_Optimal() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 5.0, 50, 2.0, 1.0, 1.5, 0.5);
        // genPct > 0.35 (35%)
        assertEquals("Status: Optimal", EnergyCalculator.determineSolarGenLevel(rec, 0.40));
    }

    @Test
    void testDetermineSolarGenLevel_NotGenerating() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 0.0, 50, 2.0, 1.0, 1.5, 0.5);
        assertEquals("Status: Not Generating", EnergyCalculator.determineSolarGenLevel(rec, 0.0));
    }

    @Test
    void testDetermineSolarGenLevel_Underperforming() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 2.0, 50, 2.0, 1.0, 1.5, 0.5);
        assertEquals("Status: Underperforming", EnergyCalculator.determineSolarGenLevel(rec, 0.20));
    }

    @Test
    void testDetermineSolarGenLevel_AtThreshold() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 3.5, 50, 2.0, 1.0, 1.5, 0.5);
        // genPct = 0.35 is not > 0.35, so it's underperforming
        assertEquals("Status: Underperforming", EnergyCalculator.determineSolarGenLevel(rec, 0.35));
    }

    @Test
    void testDetermineSolarGenLevel_JustAboveThreshold() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 4.0, 50, 2.0, 1.0, 1.5, 0.5);
        assertEquals("Status: Optimal", EnergyCalculator.determineSolarGenLevel(rec, 0.36));
    }

    @Test
    void testSolarStatusCalc_Normal() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 5.0, 50, 2.0, 1.0, 1.5, 0.5);
        ArrayList<Double> result = EnergyCalculator.solarStatusCalc(rec, 10.0);

        assertEquals(2, result.size());
        assertEquals(5.0, result.get(0), 0.001); // currentGen
        assertEquals(0.5, result.get(1), 0.001); // progressBarValue (5.0 / 10.0)
    }

    @Test
    void testSolarStatusCalc_ZeroGeneration() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 0.0, 50, 2.0, 1.0, 1.5, 0.5);
        ArrayList<Double> result = EnergyCalculator.solarStatusCalc(rec, 10.0);

        assertEquals(0.0, result.get(0), 0.001);
        assertEquals(0.0, result.get(1), 0.001);
    }

    @Test
    void testSolarStatusCalc_MaxGeneration() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 10.0, 50, 2.0, 1.0, 1.5, 0.5);
        ArrayList<Double> result = EnergyCalculator.solarStatusCalc(rec, 10.0);

        assertEquals(10.0, result.get(0), 0.001);
        assertEquals(1.0, result.get(1), 0.001);
    }

    @Test
    void testSolarStatusCalc_OverMaxCapacity() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 15.0, 50, 2.0, 1.0, 1.5, 0.5);
        ArrayList<Double> result = EnergyCalculator.solarStatusCalc(rec, 10.0);

        assertEquals(15.0, result.get(0), 0.001);
        assertEquals(1.5, result.get(1), 0.001); // Can exceed 1.0
    }

    @Test
    void testSolarStatusCalc_SmallCapacity() {
        EnergyRecord rec = new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 20.0, 100, 10.0, 2.5, 50, 2.0, 1.0, 1.5, 0.5);
        ArrayList<Double> result = EnergyCalculator.solarStatusCalc(rec, 5.0);

        assertEquals(2.5, result.get(0), 0.001);
        assertEquals(0.5, result.get(1), 0.001);
    }

    @Test
    void testCalculateCleanShare_HighSolar() {
        assertEquals(80.0, EnergyCalculator.calculateCleanShare(10.0, 8.0), 0.001);
    }

    @Test
    void testCalculateCleanShare_MoreSolarThanLoad() {
        // Solar can exceed load (excess goes to battery/grid)
        assertEquals(150.0, EnergyCalculator.calculateCleanShare(10.0, 15.0), 0.001);
    }

    @Test
    void testCalculateCost_DecimalValues() {
        assertEquals(0.5, EnergyCalculator.calculateCostInPounds(2.0, 25.0), 0.001);
    }
}
