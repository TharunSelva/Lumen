package com.energyapp.utils;

import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsightTest {

    @Test
    public void testWeeklySavingsCalculation() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(50.0, 100.0);
        assertEquals("You saved 50.00 kWh compared to the previous interval. That's a 50.0% decrease!",
                results.get(0).getDescription());
    }

    @Test
    public void testUsageIncreaseAlert() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(150.0, 100.0);
        assertEquals("Alert: Usage increased by 50.00 kWh compared to previous interval. That's a 50.0% increase!",
                results.get(0).getDescription());
    }

    @Test
    public void testZeroPreviousNoComparisonInsight() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(10.0, 0.0);
        assertEquals(1, results.size()); // only random tip
    }

    @Test
    public void testZeroDifferenceNoComparisonInsight() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(100.0, 100.0);
        assertEquals(1, results.size()); // only random tip
    }

    @Test
    public void testLargePercentageIncrease() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(10.0, 0.5);
        assertEquals("Alert: Usage increased by 9.50 kWh compared to previous interval. That's a 1900.0% increase!",
                results.get(0).getDescription());
    }

    @Test
    public void testNegativePrevious() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(50.0, -10.0);
        assertEquals(1, results.size()); // guard skips comparison
    }

    @Test
    public void testNegativeCurrent() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(-5.0, 100.0);
        // Currently produces a "Weekly Savings" with diff = 105.0, percent = 105%
        assertEquals(2, results.size()); // comparison + random tip
        assertEquals("Weekly Savings", results.get(0).getTitle());
        assertEquals("You saved 105.00 kWh compared to the previous interval. That's a 105.0% decrease!",
                results.get(0).getDescription());
    }

    @Test
    public void testZeroCurrent() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(0.0, 100.0);
        assertEquals(2, results.size());
        assertEquals("Weekly Savings", results.get(0).getTitle());
        assertEquals("You saved 100.00 kWh compared to the previous interval. That's a 100.0% decrease!",
                results.get(0).getDescription());
    }

    @Test
    public void testVerySmallPrevious() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(10.0, 0.001);
        assertEquals(2, results.size());
        assertEquals("Usage Alert", results.get(0).getTitle());
        assertEquals("Alert: Usage increased by 10.00 kWh compared to previous interval. That's a 999900.0% increase!",
                results.get(0).getDescription());
    }

    @Test
    public void testNoChange() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(100.0, 100.0);
        assertEquals(1, results.size()); // only random tip
    }

    @Test
    public void testBothZero() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(0.0, 0.0);
        assertEquals(1, results.size()); // guard skips comparison
    }

    @Test
    public void testLargeNumbers() {
        InsightEngine engine = new InsightEngine();
        List<Insight> results = engine.generateInsights(1_000_000.0, 2_000_000.0);
        assertEquals(2, results.size());
        assertEquals("Weekly Savings", results.get(0).getTitle());
        assertEquals("You saved 1000000.00 kWh compared to the previous interval. That's a 50.0% decrease!",
                results.get(0).getDescription());
    }
}