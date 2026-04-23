package com.energyapp.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenMeteoSolarServiceTest {

    private OpenMeteoSolarService solarService;
    private Method parseMethod;

    @BeforeEach
    void setUp() throws Exception {
        solarService = new OpenMeteoSolarService();

        // Access the private parse() method
        parseMethod = OpenMeteoSolarService.class.getDeclaredMethod("parse", String.class);
        parseMethod.setAccessible(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParse_ValidJsonWithNulls() throws Exception {
        // Simulating Open-Meteo JSON structure where index 1 has a null value
        String mockJson = "{"
                + "\"hourly\": {"
                + "\"time\": [\"2026-04-23T12:00\", \"2026-04-23T13:00\", \"2026-04-23T14:00\"],"
                + "\"shortwave_radiation\": [450.5, null, 600.0]"
                + "}"
                + "}";

        List<OpenMeteoSolarService.SolarPoint> results =
                (List<OpenMeteoSolarService.SolarPoint>) parseMethod.invoke(solarService, mockJson);

        assertEquals(3, results.size(), "Should parse exactly 3 data points");

        // First point normal
        assertEquals("2026-04-23T12:00", results.get(0).timeIso);
        assertEquals(450.5, results.get(0).shortwaveWm2, 0.001);

        // Second point should default to 0.0 due to isJsonNull() check in logic
        assertEquals("2026-04-23T13:00", results.get(1).timeIso);
        assertEquals(0.0, results.get(1).shortwaveWm2, 0.001, "Null radiation values should be parsed as 0.0");

        // Third point normal
        assertEquals("2026-04-23T14:00", results.get(2).timeIso);
        assertEquals(600.0, results.get(2).shortwaveWm2, 0.001);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParse_EmptyOrInvalidJson() throws Exception {
        // Missing the "hourly" object entirely
        String missingHourlyJson = "{\"latitude\": 50.9}";
        List<OpenMeteoSolarService.SolarPoint> results1 =
                (List<OpenMeteoSolarService.SolarPoint>) parseMethod.invoke(solarService, missingHourlyJson);
        assertTrue(results1.isEmpty(), "Should return empty list if 'hourly' object is missing");

        // Missing the data arrays
        String missingArraysJson = "{\"hourly\": {}}";
        List<OpenMeteoSolarService.SolarPoint> results2 =
                (List<OpenMeteoSolarService.SolarPoint>) parseMethod.invoke(solarService, missingArraysJson);
        assertTrue(results2.isEmpty(), "Should return empty list if arrays are missing");
    }
}