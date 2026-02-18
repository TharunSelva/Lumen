package com.energyapp.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
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

}
