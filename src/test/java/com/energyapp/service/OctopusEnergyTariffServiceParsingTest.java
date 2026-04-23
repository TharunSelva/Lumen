package com.energyapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OctopusEnergyTariffServiceParsingTest {

    private OctopusEnergyTariffService tariffService;
    private Method parseResultsMethod;

    @BeforeEach
    void setUp() throws Exception {
        tariffService = new OctopusEnergyTariffService();

        // Access the private parseResults() method
        parseResultsMethod = OctopusEnergyTariffService.class.getDeclaredMethod("parseResults", String.class);
        parseResultsMethod.setAccessible(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParseResults_ValidData() throws Exception {
        String mockJson = "{"
                + "\"results\": ["
                + "  {"
                + "    \"value_exc_vat\": 10.0,"
                + "    \"value_inc_vat\": 10.5,"
                + "    \"valid_from\": \"2026-04-23T12:00:00Z\","
                + "    \"valid_to\": \"2026-04-23T12:30:00Z\""
                + "  },"
                + "  {"
                + "    \"value_exc_vat\": 12.0,"
                + "    \"value_inc_vat\": 12.6,"
                + "    \"valid_from\": \"2026-04-23T13:00:00+01:00\"," // Testing timezone offset normalization
                + "    \"valid_to\": \"2026-04-23T13:30:00+01:00\""
                + "  }"
                + "]}";

        List<OctopusEnergyTariffService.UnitRate> rates =
                (List<OctopusEnergyTariffService.UnitRate>) parseResultsMethod.invoke(tariffService, mockJson);

        assertEquals(2, rates.size());

        // Check first rate (Standard UTC Z)
        assertEquals(10.5, rates.get(0).pencePerKwhIncVat, 0.001);
        assertEquals(OffsetDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC), rates.get(0).validFromUtc);

        // Check second rate (+01:00 should be normalized to UTC 12:00:00Z)
        assertEquals(12.6, rates.get(1).pencePerKwhIncVat, 0.001);
        assertEquals(OffsetDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC), rates.get(1).validFromUtc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParseResults_SkipsMalformedEntries() throws Exception {
        String mockJson = "{"
                + "\"results\": ["
                + "  {"
                + "    \"value_inc_vat\": null," // Invalid: null value
                + "    \"valid_from\": \"2026-04-23T12:00:00Z\","
                + "    \"valid_to\": \"2026-04-23T12:30:00Z\""
                + "  },"
                + "  {"
                + "    \"value_inc_vat\": 15.0,"
                + "    \"valid_from\": null,"    // Invalid: null date
                + "    \"valid_to\": \"2026-04-23T13:30:00Z\""
                + "  },"
                + "  {"
                + "    \"value_inc_vat\": 20.0," // Valid
                + "    \"valid_from\": \"2026-04-23T14:00:00Z\","
                + "    \"valid_to\": \"2026-04-23T14:30:00Z\""
                + "  }"
                + "]}";

        List<OctopusEnergyTariffService.UnitRate> rates =
                (List<OctopusEnergyTariffService.UnitRate>) parseResultsMethod.invoke(tariffService, mockJson);

        // Should have skipped the first two invalid entries and only parsed the 3rd
        assertEquals(1, rates.size(), "Should skip entries missing required non-null fields");
        assertEquals(20.0, rates.get(0).pencePerKwhIncVat, 0.001);
    }
}