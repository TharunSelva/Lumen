package com.energyapp.model;

import java.time.LocalDateTime;

public record DevicePlanCard(
        String deviceId,
        String deviceName,
        DeviceType deviceType,
        String purpose,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        String reason,
        boolean selectedPlan,
        boolean forecastBacked
) {
}
