package com.energyapp.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

public class DateTimeUtilsTest {

    @Test
    void testFormatTimeFromEpoch() {
        long epoch = 1704110400L;
        String result = DateTimeUtils.formatTimeFromEpoch(epoch);

        assertNotNull(result);
        assertEquals(5, result.length());
        assertTrue(result.contains(":"));
    }

    @Test
    void testToEpochSeconds_HandlesNull() {
        assertEquals(0, DateTimeUtils.toEpochSeconds(null));
    }

    @Test
    void testToEpochSeconds_Valid() {
        LocalDateTime now = LocalDateTime.now();
        long epoch = DateTimeUtils.toEpochSeconds(now);
        assertTrue(epoch > 0);
    }

    @Test
    void testFormatTimeFromEpoch_Midnight() {
        // Epoch for midnight UTC on Jan 1, 2024
        long midnightUtc = 1704067200L;
        String result = DateTimeUtils.formatTimeFromEpoch(midnightUtc);

        assertNotNull(result);
        assertEquals(5, result.length());
        // Verify format is HH:MM
        assertTrue(result.matches("\\d{2}:\\d{2}"));
    }

    @Test
    void testFormatTimeFromEpoch_Noon() {
        // Epoch for noon UTC on Jan 1, 2024
        long noonUtc = 1704110400L;
        String result = DateTimeUtils.formatTimeFromEpoch(noonUtc);

        assertNotNull(result);
        assertEquals(5, result.length());
    }

    @Test
    void testFormatTimeFromEpoch_ZeroEpoch() {
        String result = DateTimeUtils.formatTimeFromEpoch(0L);

        assertNotNull(result);
        assertEquals(5, result.length());
        assertTrue(result.contains(":"));
    }

    @Test
    void testFormatTimeFromEpoch_LargeEpoch() {
        // Far future date
        long largeEpoch = 2000000000L;
        String result = DateTimeUtils.formatTimeFromEpoch(largeEpoch);

        assertNotNull(result);
        assertEquals(5, result.length());
    }

    @Test
    void testToEpochSeconds_SpecificDate() {
        LocalDateTime specificDate = LocalDateTime.of(2024, 6, 15, 12, 30, 0);
        long epoch = DateTimeUtils.toEpochSeconds(specificDate);

        assertTrue(epoch > 0);
        // Verify round-trip (approximately, due to timezone)
        LocalDateTime roundTrip = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.ofTotalSeconds(
                ZoneId.systemDefault().getRules().getOffset(specificDate).getTotalSeconds()));
        assertEquals(specificDate.getHour(), roundTrip.getHour());
        assertEquals(specificDate.getMinute(), roundTrip.getMinute());
    }

    @Test
    void testToEpochSeconds_StartOfYear() {
        LocalDateTime startOfYear = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        long epoch = DateTimeUtils.toEpochSeconds(startOfYear);

        assertTrue(epoch > 0);
    }

    @Test
    void testToEpochSeconds_EndOfYear() {
        LocalDateTime endOfYear = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        long epoch = DateTimeUtils.toEpochSeconds(endOfYear);

        assertTrue(epoch > 0);
    }

    @Test
    void testToEpochSeconds_DifferentTimesOfDay() {
        LocalDateTime morning = LocalDateTime.of(2024, 6, 15, 8, 0, 0);
        LocalDateTime evening = LocalDateTime.of(2024, 6, 15, 20, 0, 0);

        long morningEpoch = DateTimeUtils.toEpochSeconds(morning);
        long eveningEpoch = DateTimeUtils.toEpochSeconds(evening);

        assertTrue(eveningEpoch > morningEpoch);
        // 12 hours difference = 43200 seconds
        assertEquals(43200, eveningEpoch - morningEpoch);
    }

    @Test
    void testToEpochSeconds_ConsecutiveDays() {
        LocalDateTime day1 = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        LocalDateTime day2 = LocalDateTime.of(2024, 6, 16, 12, 0, 0);

        long day1Epoch = DateTimeUtils.toEpochSeconds(day1);
        long day2Epoch = DateTimeUtils.toEpochSeconds(day2);

        // 24 hours difference = 86400 seconds
        assertEquals(86400, day2Epoch - day1Epoch);
    }

    @Test
    void testFormatTimeFromEpoch_ReturnFormat() {
        long epoch = 1704110400L;
        String result = DateTimeUtils.formatTimeFromEpoch(epoch);

        // Should match HH:MM format
        assertTrue(result.matches("\\d{2}:\\d{2}"));
    }

    @Test
    void testToEpochSeconds_ReturnsPositiveForRecentDate() {
        LocalDateTime recentDate = LocalDateTime.of(2020, 5, 1, 10, 30, 0);
        long epoch = DateTimeUtils.toEpochSeconds(recentDate);

        assertTrue(epoch > 0);
        assertTrue(epoch > 1500000000L); // After 2017
    }

    @Test
    void testFormatTimeFromEpoch_NegativeEpoch() {
        // Before Unix epoch (1970)
        long negativeEpoch = -86400L;
        String result = DateTimeUtils.formatTimeFromEpoch(negativeEpoch);

        assertNotNull(result);
        assertEquals(5, result.length());
    }
}
