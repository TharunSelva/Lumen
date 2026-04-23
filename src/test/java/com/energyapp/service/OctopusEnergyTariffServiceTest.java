package com.energyapp.service;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OctopusEnergyTariffServiceTest {

    @Test
    void testRoundDownToHalfHourUtc() {
        // Test standard mid-hour down-rounding
        OffsetDateTime time1 = OffsetDateTime.of(2026, 3, 5, 14, 15, 45, 0, ZoneOffset.UTC);
        OffsetDateTime expected1 = OffsetDateTime.of(2026, 3, 5, 14, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(expected1, OctopusEnergyTariffService.roundDownToHalfHourUtc(time1));

        // Test over 30 minutes rounding
        OffsetDateTime time2 = OffsetDateTime.of(2026, 3, 5, 14, 48, 12, 0, ZoneOffset.UTC);
        OffsetDateTime expected2 = OffsetDateTime.of(2026, 3, 5, 14, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(expected2, OctopusEnergyTariffService.roundDownToHalfHourUtc(time2));

        // Test boundary normalization (already on the hour)
        OffsetDateTime time3 = OffsetDateTime.of(2026, 3, 5, 15, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(time3, OctopusEnergyTariffService.roundDownToHalfHourUtc(time3));

        // Test cross-timezone normalization
        OffsetDateTime timeTz = OffsetDateTime.of(2026, 3, 5, 16, 45, 0, 0, ZoneOffset.ofHours(2));
        OffsetDateTime expectedTzUtc = OffsetDateTime.of(2026, 3, 5, 14, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(expectedTzUtc, OctopusEnergyTariffService.roundDownToHalfHourUtc(timeTz));
    }
}