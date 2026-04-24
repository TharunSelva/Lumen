package com.energyapp.model;

public enum DeviceRunMode {
    MANUAL,
    SCHEDULED;

    public static DeviceRunMode fromPersisted(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return DeviceRunMode.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
