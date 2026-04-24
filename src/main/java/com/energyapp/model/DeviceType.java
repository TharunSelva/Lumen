package com.energyapp.model;

public enum DeviceType {

    SMART_PLUG(false, "SMART_PLUG"),
    FRIDGE(false, "KITCHEN"),
    WASHING_MACHINE(true, "LAUNDRY"),
    HEATING(false, "HEATING"),
    DISHWASHER(true, "KITCHEN"),
    EV_CHARGER(true, "EV");

    private final boolean scheduled;
    private final String canonicalCategory;

    DeviceType(boolean scheduled, String canonicalCategory) {
        this.scheduled = scheduled;
        this.canonicalCategory = canonicalCategory;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public String canonicalCategory() {
        return canonicalCategory;
    }

    public static DeviceType fromCanonical(String deviceType, String category, String displayName) {
        if (deviceType != null && !deviceType.isBlank()) {
            try {
                return DeviceType.valueOf(deviceType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall back to category and display name heuristics for older rows.
            }
        }

        String normalizedName = displayName == null ? "" : displayName.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase();

        return switch (normalizedCategory) {
            case "SMART_PLUG" -> SMART_PLUG;
            case "LAUNDRY" -> WASHING_MACHINE;
            case "EV" -> EV_CHARGER;
            case "HEATING" -> HEATING;
            case "KITCHEN" -> {
                if (normalizedName.contains("dish")) {
                    yield DISHWASHER;
                }
                if (normalizedName.contains("fridge")) {
                    yield FRIDGE;
                }
                yield null;
            }
            default -> {
                if (normalizedName.contains("plug")) {
                    yield SMART_PLUG;
                }
                if (normalizedName.contains("wash")) {
                    yield WASHING_MACHINE;
                }
                if (normalizedName.contains("dish")) {
                    yield DISHWASHER;
                }
                if (normalizedName.contains("fridge")) {
                    yield FRIDGE;
                }
                if (normalizedName.contains("heat")) {
                    yield HEATING;
                }
                if (normalizedName.contains("ev")) {
                    yield EV_CHARGER;
                }
                yield null;
            }
        };
    }

}
