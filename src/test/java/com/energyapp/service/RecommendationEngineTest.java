package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new RecommendationEngine();
    }

    // checking the predicate rule solarGenKwh > 1.75
    @Test
    public void testHighSolarTriggersRecommendation() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                2.0, 50, 0.0, 0.0, 0.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Run heavy appliances now!"));
        assertTrue(containsRule);
    }

    // checking the predicate rule pricePerKwh > 25.0
    @Test
    public void testHighPriceTriggersRecommendation() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 30.0, 50.0, 0.0,
                0.0, 50, 0.0, 0.0, 0.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Delay non-essential energy use."));
        assertTrue(containsRule);
    }

    // checking the predicate rule carbon > 150.0
    @Test
    public void testHighCarbonTriggersRecommendation() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 160.0, 0.0,
                0.0, 50, 0.0, 0.0, 0.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Carbon intensity on the grid is currently high (>150gCO2/kWh)!"));
        assertTrue(containsRule);
    }

    // checking the predicate rule batteryLevel < 25
    @Test
    public void testLowBatteryTriggersRecommendation() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                0.0, 20, 0.0, 0.0, 0.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Your household battery is running low."));
        assertTrue(containsRule);
    }

    // checking the predicate rule heatingUsageKwh > 2.0
    @Test
    public void testHighHeatingTriggersRecommendation() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                0.0, 50, 0.0, 0.0, 2.5, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Heating is using a lot of energy."));
        assertTrue(containsRule);
    }

    // checking engine gives no active recommendations when the environment is neutral
    @Test
    public void testNormalConditionsTriggerNoRecommendations() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.0, 80, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertTrue(activeRecs.isEmpty());
    }
}

