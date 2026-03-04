package com.energyapp.utils;

import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsightTest {
    /**
     * Test: EC-04_InsightGeneration
     * Scope: Verifies that the InsightEngine correctly calculates the percentage
     * decrease in energy usage and chooses the 'Weekly Savings' template
     * when consumption is lower than the previous period.
     */
    @Test
    public void testWeeklySavingsCalculation() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(50.0, 100.0);
        assertEquals("You saved 50.00 kWh compared to the previous interval. That's a 50.0% decrease!",
                results.get(0).getDescription());
    }
}
