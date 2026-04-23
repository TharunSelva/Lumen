package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcoStatisticsControllerTest {

    @Test
    void buildPersonalSources_includesGridBatteryAndSolarRows() {
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.of(2026, 1, 5, 10, 0), "10:00",
                4.0, 20.0, 100.0, 10.0, 3.0, 50,
                0.0, 0.0, 0.0, 0.0
        );

        Map<String, Double> values = EcoStatisticsController.buildPersonalSources(record).stream()
                .collect(Collectors.toMap(EcoStatisticsController.SourceRow::name, EcoStatisticsController.SourceRow::value));

        assertEquals(3.0, values.get("Solar"), 0.0001);
        assertEquals(4.0, values.get("Grid"), 0.0001);
        assertEquals(3.0, values.get("Battery"), 0.0001);
    }

    @Test
    void buildPersonalSources_keepsZeroRowsWhenNoRecordExists() {
        var rows = EcoStatisticsController.buildPersonalSources(null);

        assertEquals(EcoStatisticsController.PERSONAL_SOURCE_NAMES, rows.stream().map(EcoStatisticsController.SourceRow::name).toList());
        assertTrue(rows.stream().allMatch(row -> row.value() == 0.0));
    }

    @Test
    void buildGridSources_includesAllFetchedFuelNamesIncludingZeroValues() {
        Map<String, Double> lookup = Map.of(
                "biomass", 1.0,
                "gas", 35.0,
                "wind", 20.0
        );

        var rows = EcoStatisticsController.buildGridSources(fuel -> lookup.getOrDefault(fuel, 0.0));

        assertEquals(9, rows.size());
        assertEquals("Biomass", rows.get(0).name());
        assertEquals("Coal", rows.get(1).name());
        assertEquals("Imports", rows.get(2).name());
        assertEquals("Gas", rows.get(3).name());
        assertEquals("Nuclear", rows.get(4).name());
        assertEquals("Other", rows.get(5).name());
        assertEquals("Hydro", rows.get(6).name());
        assertEquals("Solar", rows.get(7).name());
        assertEquals("Wind", rows.get(8).name());
        assertEquals(0.0, rows.get(1).value(), 0.0001);
    }

    @Test
    void determineHourStatus_mapsThresholdsToRecommendations() {
        assertEquals("Low carbon hour", EcoStatisticsController.determineHourStatus(50).status());
        assertEquals("Moderate carbon hour", EcoStatisticsController.determineHourStatus(150).status());
        assertEquals("High carbon hour", EcoStatisticsController.determineHourStatus(250).status());
    }
}
