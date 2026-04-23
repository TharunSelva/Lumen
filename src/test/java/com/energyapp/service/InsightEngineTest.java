package com.energyapp.service;

import com.energyapp.db.QuickTipRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Insight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class InsightEngineTest {

    private InsightEngine insightEngine;

    @BeforeEach
    void setUp() {
        // Using the default constructor which handles its own tipRepository
        insightEngine = new InsightEngine();
    }

    @Test
    void testGenerateInsights_PlainEnglishSavings() {
        // Test EC-04: Plain-english explanations of weekly savings
        // User used 80 kWh this week, 100 kWh last week
        List<Insight> insights = insightEngine.generateInsights(80.0, 100.0);

        Insight savingsInsight = insights.stream()
                .filter(i -> i.getTitle().equals("Weekly Savings"))
                .findFirst()
                .orElse(null);

        assertNotNull(savingsInsight, "Should generate a Weekly Savings insight");
        assertTrue(savingsInsight.getDescription().contains("You saved 20.00 kWh"), "Should explain absolute savings");
        assertTrue(savingsInsight.getDescription().contains("20.0% decrease"), "Should explain percentage savings");
    }

    @Test
    void testGenerateInsights_UsageAlert() {
        // User used 120 kWh this week, 100 kWh last week
        List<Insight> insights = insightEngine.generateInsights(120.0, 100.0);

        Insight alertInsight = insights.stream()
                .filter(i -> i.getTitle().equals("Usage Alert"))
                .findFirst()
                .orElse(null);

        assertNotNull(alertInsight, "Should generate a Usage Alert insight");
        assertTrue(alertInsight.getDescription().contains("increased by 20.00 kWh"));
        assertTrue(alertInsight.getDescription().contains("20.0% increase"));
    }

    @Test
    void testGenerateGreenForecastInsight_FindsOptimalWindow() throws Exception {
        // Test EN-03: Green forecast window allocation
        List<EnergyRecord> mockRecords = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.of(2026, 4, 21, 12, 0);

        // Window 1 (12:00 - 13:30): High price, high carbon
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime, 30.0, 150.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(30), 32.0, 160.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(60), 35.0, 170.0));

        // Window 2 (13:30 - 15:00): LOWEST price, LOWEST carbon (Optimal Window)
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(90), 10.0, 50.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(120), 8.0, 40.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(150), 9.0, 45.0));

        // Window 3 (15:00 - 16:30): Medium price, Medium carbon
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(180), 20.0, 100.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(210), 21.0, 105.0));
        mockRecords.add(createRecordWithPriceAndCarbon(baseTime.plusMinutes(240), 19.0, 95.0));

        Insight greenInsight = insightEngine.generateGreenForecastInsight(mockRecords);

        assertNotNull(greenInsight);
        assertEquals("Green Forecast", greenInsight.getTitle());
        // Should explicitly identify the 13:30 window start time
        assertTrue(greenInsight.getDescription().contains("13:30 - 15:00"),
                "Engine should highlight the cheapest/greenest combined 90-minute window");
    }

    @Test
    void testGenerateGreenForecastInsight_NotEnoughData() {
        List<EnergyRecord> shortList = List.of(
                createRecordWithPriceAndCarbon(LocalDateTime.now(), 10.0, 50.0),
                createRecordWithPriceAndCarbon(LocalDateTime.now().plusMinutes(30), 10.0, 50.0)
        );
        // Requires at least 3 records (90 mins) to calculate a window
        assertNull(insightEngine.generateGreenForecastInsight(shortList));
    }

    // Helper to bypass constructor argument length mismatches across versions and inject specific test params
    private EnergyRecord createRecordWithPriceAndCarbon(LocalDateTime time, double price, double carbon) {
        // Utilizing the 12-param constructor seen in the DashboardTest
        EnergyRecord record = new EnergyRecord(
                time, time.toLocalTime().toString(), 0.0, price, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0
        );
        try {
            Field carbonField = EnergyRecord.class.getDeclaredField("carbon");
            carbonField.setAccessible(true);
            carbonField.set(record, carbon);
        } catch (Exception e) {
            // Ignore for test
        }
        return record;
    }
}