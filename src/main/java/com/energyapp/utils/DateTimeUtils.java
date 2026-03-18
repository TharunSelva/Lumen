package com.energyapp.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Utility class for calculating things about LocalTimes.
 */
public final class DateTimeUtils {

    public static String formatTimeFromEpoch(long epochSeconds) {
        LocalTime t = Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
        // this returns the substring containing hours and minutes
        return t.toString().substring(0, 5);
    }

    public static long toEpochSeconds(LocalDateTime time) {
        if (time == null) return 0;
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

}
