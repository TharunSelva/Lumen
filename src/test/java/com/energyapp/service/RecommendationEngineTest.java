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

    // Additional test cases

    @Test
    public void testMultipleConditionsTriggered() {
        // High solar (>1.75), high price (>25), high carbon (>150), low battery (<25), high heating (>2.0)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 30.0, 160.0, 0.0,
                2.0, 20, 0.0, 0.0, 2.5, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertEquals(5, activeRecs.size(), "All 5 recommendations should be triggered");
    }

    @Test
    public void testSolarAtExactThreshold() {
        // solar = 1.75 (not > 1.75, so should NOT trigger)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.75, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Run heavy appliances now!"));
        assertFalse(containsRule, "Solar at exactly 1.75 should not trigger recommendation");
    }

    @Test
    public void testPriceAtExactThreshold() {
        // price = 25.0 (not > 25, so should NOT trigger)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 25.0, 50.0, 0.0,
                1.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Delay non-essential energy use."));
        assertFalse(containsRule, "Price at exactly 25.0 should not trigger recommendation");
    }

    @Test
    public void testCarbonAtExactThreshold() {
        // carbon = 150.0 (not > 150, so should NOT trigger)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 150.0, 0.0,
                1.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Carbon intensity on the grid is currently high (>150gCO2/kWh)!"));
        assertFalse(containsRule, "Carbon at exactly 150.0 should not trigger recommendation");
    }

    @Test
    public void testBatteryAtExactThreshold() {
        // battery = 25 (not < 25, so should NOT trigger)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.0, 25, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Your household battery is running low."));
        assertFalse(containsRule, "Battery at exactly 25 should not trigger recommendation");
    }

    @Test
    public void testHeatingAtExactThreshold() {
        // heating = 2.0 (not > 2.0, so should NOT trigger)
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.0, 50, 0.0, 0.0, 2.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Heating is using a lot of energy."));
        assertFalse(containsRule, "Heating at exactly 2.0 should not trigger recommendation");
    }

    @Test
    public void testZeroBatteryLevel() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.0, 0, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Your household battery is running low."));
        assertTrue(containsRule, "Zero battery should trigger low battery recommendation");
    }

    @Test
    public void testFullBatteryLevel() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                1.0, 100, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Your household battery is running low."));
        assertFalse(containsRule, "Full battery should not trigger low battery recommendation");
    }

    @Test
    public void testVerySolarProduction() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                10.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Run heavy appliances now!"));
        assertTrue(containsRule);
    }

    @Test
    public void testZeroSolarProduction() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 15.0, 50.0, 0.0,
                0.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Run heavy appliances now!"));
        assertFalse(containsRule, "Zero solar should not trigger high solar recommendation");
    }

    @Test
    public void testVeryHighPrice() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 100.0, 50.0, 0.0,
                1.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Delay non-essential energy use."));
        assertTrue(containsRule);
    }

    @Test
    public void testZeroPrice() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 0.0, 50.0, 0.0,
                1.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);
        boolean containsRule = activeRecs.stream()
                .anyMatch(rec -> rec.getTitle().equals("Delay non-essential energy use."));
        assertFalse(containsRule, "Zero price should not trigger high price recommendation");
    }

    @Test
    public void testRecommendationExplanationsExist() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 30.0, 160.0, 0.0,
                2.0, 20, 0.0, 0.0, 2.5, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        for (Recommendation rec : activeRecs) {
            assertNotNull(rec.getExplanation());
            assertFalse(rec.getExplanation().isEmpty());
        }
    }

    @Test
    public void testRecommendationConditionsAccessible() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, 30.0, 50.0, 0.0,
                1.0, 50, 0.0, 0.0, 1.0, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        for (Recommendation rec : activeRecs) {
            assertNotNull(rec.getCondition());
        }
    }

    @Test
    public void testRecommendationsExposeStableIdsAndTimestamps() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 12, 0), "12:00", 0.0, 30.0, 160.0, 0.0,
                2.0, 20, 0.0, 0.0, 2.5, 0.0
        );

        List<Recommendation> activeRecs = engine.getActiveRecommendations(record);

        assertFalse(activeRecs.isEmpty());
        for (Recommendation rec : activeRecs) {
            assertNotNull(rec.getId());
            assertNotNull(rec.getShownAt());
            assertTrue(rec.isActive());
        }
    }
}


