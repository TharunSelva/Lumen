package com.energyapp.db;

import java.time.LocalDateTime;

public record ForecastWindowRecord(
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        Double tariffPPerKwh,
        Double carbonGco2PerKwh,
        Double solarShortwaveWm2,
        Double batterySocPctPred,
        String batteryAction
) {
}
