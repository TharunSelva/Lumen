package com.energyapp.model;

public enum DeviceStatus {
    OFF,
    IDLE,
    SCHEDULED,
    RUNNING,
    SKIPPED;

    public static DeviceStatus fromPersisted(String rawValue, boolean isOn) {
        if (rawValue != null && !rawValue.isBlank()) {
            try {
                return DeviceStatus.valueOf(rawValue.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to sensible defaults when legacy rows contain unknown values.
            }
        }
        return isOn ? IDLE : OFF;
    }
}
