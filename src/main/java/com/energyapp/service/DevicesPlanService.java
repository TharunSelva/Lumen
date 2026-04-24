package com.energyapp.service;

import com.energyapp.db.ForecastCacheRepository;
import com.energyapp.db.ForecastIntervalRepository;
import com.energyapp.db.ForecastWindowRecord;
import com.energyapp.model.Device;
import com.energyapp.model.DevicePlanCard;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.DeviceType;
import com.energyapp.model.ScheduledDevice;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DevicesPlanService {

    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter DETAIL_FMT = DateTimeFormatter.ofPattern("EEE HH:mm");

    private final ForecastCacheRepository forecastCacheRepository;
    private final ForecastIntervalRepository forecastIntervalRepository;
    private final String homeId;

    public DevicesPlanService() {
        this(new ForecastCacheRepository(), new ForecastIntervalRepository(), HOME_ID);
    }

    DevicesPlanService(
            ForecastCacheRepository forecastCacheRepository,
            ForecastIntervalRepository forecastIntervalRepository,
            String homeId
    ) {
        this.forecastCacheRepository = forecastCacheRepository;
        this.forecastIntervalRepository = forecastIntervalRepository;
        this.homeId = homeId;
    }

    public List<DevicePlanCard> buildPlanCards(List<Device> devices, LocalDateTime referenceTime) {
        return buildPlanCards(devices, referenceTime, Map.of());
    }

    public List<DevicePlanCard> buildPlanCards(
            List<Device> devices,
            LocalDateTime referenceTime,
            Map<String, LocalDateTime> selectedPlans
    ) {
        if (referenceTime == null) {
            referenceTime = LocalDateTime.now();
        }

        ZoneId homeZone = forecastIntervalRepository.resolveHomeZone(homeId);
        List<ForecastWindowRecord> forecastWindow = loadForecastWindow(referenceTime, homeZone);

        double averagePrice = forecastWindow.stream()
                .map(ForecastWindowRecord::tariffPPerKwh)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double averageCarbon = forecastWindow.stream()
                .map(ForecastWindowRecord::carbonGco2PerKwh)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double averageSolar = forecastWindow.stream()
                .map(ForecastWindowRecord::solarShortwaveWm2)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        List<DevicePlanCard> cards = new ArrayList<>();
        for (Device device : devices) {
            if (device instanceof ScheduledDevice scheduledDevice && !scheduledDevice.isSkipNextRun()) {
                DevicePlanCard card = buildScheduledCard(
                        scheduledDevice,
                        forecastWindow,
                        referenceTime,
                        averagePrice,
                        averageCarbon,
                        averageSolar,
                        selectedPlans
                );
                if (card != null) {
                    cards.add(card);
                }
            }
        }

        cards.sort(Comparator.comparing(DevicePlanCard::windowStart, Comparator.nullsLast(Comparator.naturalOrder())));
        return cards;
    }

    private List<ForecastWindowRecord> loadForecastWindow(LocalDateTime referenceTime, ZoneId homeZone) {
        List<ForecastWindowRecord> forecastWindow = fetchCacheWindow(referenceTime, homeZone);
        if (hasForecastData(forecastWindow)) {
            return forecastWindow;
        }

        LocalDateTime runtimeNow = LocalDateTime.now(homeZone);
        if (!runtimeNow.toLocalDate().equals(referenceTime.toLocalDate())) {
            forecastWindow = fetchCacheWindow(runtimeNow, homeZone);
            if (hasForecastData(forecastWindow)) {
                return forecastWindow;
            }
        }

        forecastWindow = fetchTariffForecastWindow(referenceTime, homeZone);
        if (hasForecastData(forecastWindow)) {
            return forecastWindow;
        }

        if (!runtimeNow.toLocalDate().equals(referenceTime.toLocalDate())) {
            forecastWindow = fetchTariffForecastWindow(runtimeNow, homeZone);
        }
        return forecastWindow;
    }

    private List<ForecastWindowRecord> fetchCacheWindow(LocalDateTime referenceTime, ZoneId homeZone) {
        return forecastCacheRepository.fetchForecastWindow(
                homeId,
                referenceTime,
                referenceTime.plusHours(24),
                homeZone
        );
    }

    private boolean hasForecastData(List<ForecastWindowRecord> forecastWindow) {
        return forecastWindow.stream().anyMatch(window ->
                window.tariffPPerKwh() != null
                        || window.carbonGco2PerKwh() != null
                        || window.solarShortwaveWm2() != null
        );
    }

    private DevicePlanCard buildScheduledCard(
            ScheduledDevice device,
            List<ForecastWindowRecord> forecastWindow,
            LocalDateTime referenceTime,
            double averagePrice,
            double averageCarbon,
            double averageSolar,
            Map<String, LocalDateTime> selectedPlans
    ) {
        if (!hasForecastData(forecastWindow)) {
            LocalDateTime fallbackStart = fallbackScheduledStart(device, referenceTime);
            LocalDateTime fallbackEnd = fallbackStart == null ? null : fallbackStart.plus(resolveRunDuration(device));
            return new DevicePlanCard(
                    device.getDeviceId(),
                    device.getDeviceName(),
                device.getDeviceType(),
                buildPurpose(device.getDeviceType(), fallbackStart),
                fallbackStart,
                fallbackEnd,
                fallbackReason(device, fallbackStart),
                isSelectedPlan(device.getDeviceId(), fallbackStart, selectedPlans),
                false
            );
        }

        List<ForecastWindowRecord> candidates = switch (device.getDeviceType()) {
            case EV_CHARGER -> forecastWindow.stream()
                    .filter(window -> isOvernight(window.windowStart()))
                    .toList();
            case DISHWASHER -> forecastWindow.stream()
                    .filter(window -> isMidday(window.windowStart()))
                    .toList();
            default -> forecastWindow;
        };

        if (candidates.isEmpty()) {
            candidates = forecastWindow;
        }

        ForecastWindowRecord bestWindow = candidates.stream()
                .min(Comparator.comparingDouble(window -> scoreWindow(device, window, averagePrice, averageCarbon)))
                .orElse(null);

        if (bestWindow == null || bestWindow.windowStart() == null) {
            return null;
        }

        LocalDateTime windowStart = bestWindow.windowStart();
        LocalDateTime windowEnd = windowStart.plus(resolveRunDuration(device));
        return new DevicePlanCard(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getDeviceType(),
                buildPurpose(device.getDeviceType(), windowStart),
                windowStart,
                windowEnd,
                buildReason(device, bestWindow, averagePrice, averageCarbon, averageSolar),
                isSelectedPlan(device.getDeviceId(), windowStart, selectedPlans),
                true
        );
    }

    private List<ForecastWindowRecord> fetchTariffForecastWindow(LocalDateTime referenceTime, ZoneId homeZone) {
        LocalDate startDate = referenceTime.toLocalDate();
        LocalDate endDate = referenceTime.plusHours(24).toLocalDate();
        List<ForecastWindowRecord> windows = new ArrayList<>();

        windows.addAll(toTariffWindow(
                forecastIntervalRepository.fetchTariffForecast(homeId, startDate, homeZone)
        ));
        if (!endDate.equals(startDate)) {
            windows.addAll(toTariffWindow(
                    forecastIntervalRepository.fetchTariffForecast(homeId, endDate, homeZone)
            ));
        }

        return windows.stream()
                .filter(window -> window.windowStart() != null)
                .filter(window -> !window.windowStart().isBefore(referenceTime))
                .filter(window -> window.windowStart().isBefore(referenceTime.plusHours(24)))
                .sorted(Comparator.comparing(ForecastWindowRecord::windowStart))
                .toList();
    }

    private List<ForecastWindowRecord> toTariffWindow(List<EnergyRecord> records) {
        return records.stream()
                .filter(record -> record.getTimestamp() != null)
                .map(record -> new ForecastWindowRecord(
                        record.getTimestamp(),
                        record.getTimestamp().plusMinutes(30),
                        record.getPricePerKwh(),
                        record.getCarbon(),
                        null,
                        null,
                        null
                ))
                .toList();
    }

    private double scoreWindow(
            ScheduledDevice device,
            ForecastWindowRecord window,
            double averagePrice,
            double averageCarbon
    ) {
        double price = window.tariffPPerKwh() == null ? averagePrice : window.tariffPPerKwh();
        double carbon = window.carbonGco2PerKwh() == null ? averageCarbon : window.carbonGco2PerKwh();
        double solar = window.solarShortwaveWm2() == null ? 0.0 : window.solarShortwaveWm2();

        double priceWeight = device.isGreenEnergyOnly() ? 0.6 : 1.0;
        double carbonWeight = device.isGreenEnergyOnly() ? 0.09 : 0.05;
        double solarWeight = device.isGreenEnergyOnly() ? 0.05 : 0.02;

        return (price * priceWeight) + (carbon * carbonWeight) - (solar * solarWeight);
    }

    private String buildPurpose(DeviceType deviceType, LocalDateTime windowStart) {
        return switch (deviceType) {
            case EV_CHARGER -> "Charge the EV in the best overnight slot";
            case DISHWASHER -> "Run the dishwasher in a cleaner daytime slot";
            case WASHING_MACHINE -> "Run the washing machine in the best upcoming slot";
            default -> "Use the next best run window";
        };
    }

    private String buildReason(
            ScheduledDevice device,
            ForecastWindowRecord window,
            double averagePrice,
            double averageCarbon,
            double averageSolar
    ) {
        String timeLabel = window.windowStart().format(DETAIL_FMT);
        String priceText = formatPrice(window.tariffPPerKwh());
        boolean cheaper = window.tariffPPerKwh() != null && window.tariffPPerKwh() <= averagePrice;
        boolean cleaner = window.carbonGco2PerKwh() != null && window.carbonGco2PerKwh() <= averageCarbon;
        boolean sunnier = window.solarShortwaveWm2() != null && window.solarShortwaveWm2() > 0.0
                && window.solarShortwaveWm2() >= averageSolar;

        if (device.isGreenEnergyOnly()) {
            if (sunnier) {
                return "Recommended for " + timeLabel + " at " + priceText + " because this is one of the cleaner, solar-richer slots ahead.";
            }
            if (cleaner) {
                return "Recommended for " + timeLabel + " at " + priceText + " because carbon is lower than most of the next 24 hours.";
            }
            return "Recommended for " + timeLabel + " at " + priceText + " because it is the best greener window available in the next 24 hours.";
        }

        return switch (device.getDeviceType()) {
            case EV_CHARGER -> "Recommended for " + timeLabel + " at " + priceText + " because it is the best overnight charging slot ahead.";
            case DISHWASHER -> sunnier
                    ? "Recommended for " + timeLabel + " at " + priceText + " because it is cheaper and lines up with stronger daytime solar."
                    : "Recommended for " + timeLabel + " at " + priceText + " because it is one of the cleaner daytime dishwasher windows.";
            case WASHING_MACHINE -> cheaper && cleaner
                    ? "Recommended for " + timeLabel + " at " + priceText + " because it is cheaper and cleaner than most nearby hours."
                    : cheaper
                    ? "Recommended for " + timeLabel + " at " + priceText + " because it is cheaper than most of the next 24 hours."
                    : cleaner
                    ? "Recommended for " + timeLabel + " at " + priceText + " because it is a lower-carbon laundry slot."
                    : "Recommended for " + timeLabel + " at " + priceText + " because it is the best next laundry slot available.";
            default -> cheaper
                    ? "Recommended for " + timeLabel + " at " + priceText + " because it is one of the cheaper run windows ahead."
                    : "Recommended for " + timeLabel + " at " + priceText + " because it is the best available slot in the next 24 hours.";
        };
    }

    private LocalDateTime fallbackScheduledStart(ScheduledDevice device, LocalDateTime referenceTime) {
        if (device.getSchedule() == null) {
            return null;
        }
        LocalDateTime scheduledStart = referenceTime.toLocalDate().atTime(device.getSchedule());
        if (scheduledStart.isBefore(referenceTime)) {
            scheduledStart = scheduledStart.plusDays(1);
        }
        return scheduledStart;
    }

    private boolean isOvernight(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        int hour = timestamp.getHour();
        return hour >= 22 || hour < 6;
    }

    private boolean isMidday(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        int hour = timestamp.getHour();
        return hour >= 10 && hour <= 15;
    }

    private boolean isSelectedPlan(
            String deviceId,
            LocalDateTime windowStart,
            Map<String, LocalDateTime> selectedPlans
    ) {
        if (deviceId == null || windowStart == null || selectedPlans == null) {
            return false;
        }
        return windowStart.equals(selectedPlans.get(deviceId));
    }

    private String fallbackReason(ScheduledDevice device, LocalDateTime fallbackStart) {
        if (fallbackStart == null) {
            return "Forecast unavailable right now. Set a saved run time in device settings to create a plan.";
        }
        return "Forecast unavailable right now; this is your saved run time.";
    }

    private String formatPrice(Double value) {
        if (value == null) {
            return "an unknown price";
        }
        return String.format("%.1f p/kWh", value);
    }

    private Duration resolveRunDuration(ScheduledDevice device) {
        if (device == null || device.getDeviceType() == null) {
            return Duration.ofHours(1);
        }
        return switch (device.getDeviceType()) {
            case WASHING_MACHINE, DISHWASHER -> Duration.ofHours(2);
            case EV_CHARGER -> Duration.ofHours(4);
            default -> Duration.ofHours(1);
        };
    }
}
