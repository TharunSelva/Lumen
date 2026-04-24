package com.energyapp.db;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceRunMode;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.DeviceType;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.utils.DeviceFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class CanonicalDeviceMapper {

    private static final DateTimeFormatter DB_TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DB_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private CanonicalDeviceMapper() {
    }

    public static Device toDevice(CanonicalDeviceRecord record) {
        DeviceType deviceType = DeviceType.fromCanonical(record.deviceType(), record.category(), record.displayName());
        if (deviceType == null) {
            return null;
        }

        Device device = DeviceFactory.createDevice(record.displayName(), deviceType);
        if (device == null) {
            return null;
        }

        device.setDeviceId(record.deviceId());
        device.setOn(!"OFF".equalsIgnoreCase(record.powerState()));
        device.setStatus(DeviceStatus.fromPersisted(record.status(), device.isOn()));
        device.setGreenEnergyOnly(record.greenEnergyOnly());
        device.setActiveRunUntil(parseDateTime(record.activeRunUntil()));
        device.setRunMode(DeviceRunMode.fromPersisted(record.runMode()));
        device.setLastRunStartedAt(parseDateTime(record.lastRunStartedAt()));
        device.setLastRunEndedAt(parseDateTime(record.lastRunEndedAt()));

        if (device instanceof SmartPlug smartPlug && record.priceCutoffPPerKwh() != null) {
            smartPlug.setPriceCutoff(record.priceCutoffPPerKwh());
        }

        if (device instanceof ScheduledDevice scheduledDevice) {
            scheduledDevice.setSchedule(parseTime(record.scheduleTime()));
            scheduledDevice.setSkipNextRun(record.skipNextRun());
            scheduledDevice.setLastScheduledRunDate(parseDate(record.lastScheduledRunDate()));
        }

        return device;
    }

    public static CanonicalDeviceRecord newRecord(Device device, String homeId, LocalDateTime timestamp) {
        String formattedTimestamp = formatDateTime(timestamp);
        DeviceType deviceType = device.getDeviceType();
        String category = deviceType.canonicalCategory();
        String iconKey = defaultIconKey(deviceType);
        Double typicalPowerW = defaultTypicalPowerW(deviceType);
        Double typicalCycleKwh = defaultTypicalCycleKwh(deviceType);
        Integer typicalDurationMins = defaultTypicalDurationMins(deviceType);
        String policy = defaultPolicy(deviceType);

        return new CanonicalDeviceRecord(
                device.getDeviceId(),
                homeId,
                device.getDeviceName(),
                deviceType.name(),
                category,
                iconKey,
                typicalPowerW,
                typicalCycleKwh,
                typicalDurationMins,
                powerState(device),
                statusValue(device),
                currentDraw(device, typicalPowerW, null),
                device.isGreenEnergyOnly(),
                null,
                scheduleValue(device),
                cutoffValue(device),
                policy,
                null,
                null,
                null,
                null,
                null,
                null,
                formatDateTime(device.getActiveRunUntil()),
                runModeValue(device),
                formatDateTime(device.getLastRunStartedAt()),
                formatDateTime(device.getLastRunEndedAt()),
                lastScheduledDateValue(device),
                skipNextRunValue(device),
                null,
                formattedTimestamp,
                formattedTimestamp,
                null
        );
    }

    public static CanonicalDeviceRecord merge(CanonicalDeviceRecord existing, Device device, LocalDateTime updatedAt) {
        DeviceType deviceType = device.getDeviceType();
        String category = existing.category() == null || existing.category().isBlank()
                ? deviceType.canonicalCategory()
                : existing.category();
        String iconKey = existing.iconKey() == null || existing.iconKey().isBlank()
                ? defaultIconKey(deviceType)
                : existing.iconKey();
        Double typicalPowerW = existing.typicalPowerW() == null ? defaultTypicalPowerW(deviceType) : existing.typicalPowerW();
        Double typicalCycleKwh = existing.typicalCycleKwh() == null ? defaultTypicalCycleKwh(deviceType) : existing.typicalCycleKwh();
        Integer typicalDurationMins = existing.typicalDurationMins() == null
                ? defaultTypicalDurationMins(deviceType)
                : existing.typicalDurationMins();
        String policy = existing.policy() == null || existing.policy().isBlank()
                ? defaultPolicy(deviceType)
                : existing.policy();

        return new CanonicalDeviceRecord(
                existing.deviceId(),
                existing.homeId(),
                device.getDeviceName(),
                deviceType.name(),
                category,
                iconKey,
                typicalPowerW,
                typicalCycleKwh,
                typicalDurationMins,
                powerState(device),
                statusValue(device),
                currentDraw(device, typicalPowerW, existing.currentDrawW()),
                device.isGreenEnergyOnly(),
                existing.readyByTime(),
                scheduleValue(device),
                cutoffValue(device),
                policy,
                existing.nextRunWindowStart(),
                existing.nextRunWindowEnd(),
                existing.nextRunEstCostGbp(),
                existing.nextRunEstCo2Kg(),
                existing.nextRunSourceSolarPct(),
                existing.nextRunSourceGridPct(),
                formatDateTime(device.getActiveRunUntil()),
                runModeValue(device),
                formatDateTime(device.getLastRunStartedAt()),
                formatDateTime(device.getLastRunEndedAt()),
                lastScheduledDateValue(device),
                skipNextRunValue(device),
                existing.locationLabel(),
                existing.createdAt() == null ? formatDateTime(updatedAt) : existing.createdAt(),
                formatDateTime(updatedAt),
                existing.deletedAt()
        );
    }

    private static String scheduleValue(Device device) {
        if (device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getSchedule() != null) {
            return scheduledDevice.getSchedule().format(DB_TIME_FMT);
        }
        return null;
    }

    private static Double cutoffValue(Device device) {
        if (device instanceof SmartPlug smartPlug) {
            return smartPlug.getPriceCutoff();
        }
        return null;
    }

    private static String lastScheduledDateValue(Device device) {
        if (device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getLastScheduledRunDate() != null) {
            return scheduledDevice.getLastScheduledRunDate().toString();
        }
        return null;
    }

    private static boolean skipNextRunValue(Device device) {
        return device instanceof ScheduledDevice scheduledDevice && scheduledDevice.isSkipNextRun();
    }

    private static String runModeValue(Device device) {
        return device.getRunMode() == null ? null : device.getRunMode().name();
    }

    private static String powerState(Device device) {
        return device.isOn() ? "ON" : "OFF";
    }

    private static String statusValue(Device device) {
        DeviceStatus status = device.getStatus();
        if (status == null) {
            return device.isOn() ? DeviceStatus.IDLE.name() : DeviceStatus.OFF.name();
        }
        return status.name();
    }

    private static Double currentDraw(Device device, Double typicalPowerW, Double existingCurrentDraw) {
        if (!device.isOn()) {
            return 0.0;
        }
        if (device.getStatus() == DeviceStatus.RUNNING && typicalPowerW != null) {
            return typicalPowerW;
        }
        return existingCurrentDraw;
    }

    private static String defaultIconKey(DeviceType deviceType) {
        return switch (deviceType) {
            case SMART_PLUG -> "plug";
            case FRIDGE -> "fridge";
            case WASHING_MACHINE -> "washer";
            case HEATING -> "heat";
            case DISHWASHER -> "dishwasher";
            case EV_CHARGER -> "ev";
        };
    }

    private static Double defaultTypicalPowerW(DeviceType deviceType) {
        return switch (deviceType) {
            case SMART_PLUG -> null;
            case FRIDGE -> 150.0;
            case WASHING_MACHINE -> 2000.0;
            case HEATING -> 3000.0;
            case DISHWASHER -> 1800.0;
            case EV_CHARGER -> 7000.0;
        };
    }

    private static Double defaultTypicalCycleKwh(DeviceType deviceType) {
        return switch (deviceType) {
            case SMART_PLUG -> null;
            case FRIDGE -> 0.8;
            case WASHING_MACHINE -> 1.4;
            case HEATING -> 2.1;
            case DISHWASHER -> 1.2;
            case EV_CHARGER -> 3.5;
        };
    }

    private static Integer defaultTypicalDurationMins(DeviceType deviceType) {
        return switch (deviceType) {
            case SMART_PLUG -> null;
            case FRIDGE -> 1440;
            case WASHING_MACHINE -> 90;
            case HEATING -> 180;
            case DISHWASHER -> 120;
            case EV_CHARGER -> 120;
        };
    }

    private static String defaultPolicy(DeviceType deviceType) {
        return switch (deviceType) {
            case EV_CHARGER -> "MIN_COST";
            case SMART_PLUG, FRIDGE, WASHING_MACHINE, HEATING, DISHWASHER -> "BALANCED";
        };
    }

    private static LocalDateTime parseDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(rawValue, DB_TS_FMT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static LocalTime parseTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(rawValue, DB_TIME_FMT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static LocalDate parseDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DB_TS_FMT);
    }
}
