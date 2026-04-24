package com.energyapp.service;

import com.energyapp.db.ForecastCacheRepository;
import com.energyapp.db.ForecastIntervalRepository;
import com.energyapp.db.ForecastWindowRecord;
import com.energyapp.db.HomeEnergyAssetsRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class ForecastOverviewService {

    public static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ForecastCacheRepository forecastCacheRepository;
    private final ForecastIntervalRepository forecastIntervalRepository;
    private final HomeEnergyAssetsRepository homeEnergyAssetsRepository;
    private final Clock clock;

    public ForecastOverviewService() {
        this(
                new ForecastCacheRepository(),
                new ForecastIntervalRepository(),
                new HomeEnergyAssetsRepository(),
                Clock.systemDefaultZone()
        );
    }

    ForecastOverviewService(
            ForecastCacheRepository forecastCacheRepository,
            ForecastIntervalRepository forecastIntervalRepository,
            HomeEnergyAssetsRepository homeEnergyAssetsRepository,
            Clock clock
    ) {
        this.forecastCacheRepository = forecastCacheRepository;
        this.forecastIntervalRepository = forecastIntervalRepository;
        this.homeEnergyAssetsRepository = homeEnergyAssetsRepository;
        this.clock = clock;
    }

    public ForecastOverviewSnapshot load(String homeId, LocalDateTime preferredReferenceTime) {
        ZoneId homeZone = forecastIntervalRepository.resolveHomeZone(homeId);
        HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets = homeEnergyAssetsRepository.fetchByHomeId(homeId);

        LocalDateTime requestedAnchor = preferredReferenceTime != null
                ? preferredReferenceTime
                : LocalDateTime.now(clock.withZone(homeZone));

        List<ForecastWindowRecord> requestedUpcoming = forecastCacheRepository.fetchForecastWindow(
                homeId,
                requestedAnchor,
                requestedAnchor.plusHours(12),
                homeZone
        );

        LocalDateTime effectiveAnchor = requestedAnchor;
        List<ForecastWindowRecord> upcomingWindows = requestedUpcoming;
        if (requestedUpcoming.isEmpty()) {
            LocalDateTime firstCachedWindow = forecastCacheRepository.findFirstWindowStart(homeId, homeZone);
            if (firstCachedWindow != null) {
                effectiveAnchor = firstCachedWindow;
                upcomingWindows = forecastCacheRepository.fetchForecastWindow(
                        homeId,
                        effectiveAnchor,
                        effectiveAnchor.plusHours(12),
                        homeZone
                );
            }
        }

        LocalDate anchorDate = effectiveAnchor.toLocalDate();
        LocalDateTime anchorDayStart = anchorDate.atStartOfDay();
        LocalDateTime tomorrowStart = anchorDate.plusDays(1).atStartOfDay();

        List<ForecastWindowRecord> anchorDayWindows = forecastCacheRepository.fetchForecastWindow(
                homeId,
                anchorDayStart,
                anchorDayStart.plusDays(1),
                homeZone
        );
        List<ForecastWindowRecord> tomorrowWindows = forecastCacheRepository.fetchForecastWindow(
                homeId,
                tomorrowStart,
                tomorrowStart.plusDays(1),
                homeZone
        );

        return new ForecastOverviewSnapshot(
                effectiveAnchor,
                homeZone,
                homeEnergyAssets,
                List.copyOf(upcomingWindows),
                List.copyOf(anchorDayWindows),
                List.copyOf(tomorrowWindows)
        );
    }

    public TomorrowSummarySnapshot buildTomorrowSummary(ForecastOverviewSnapshot snapshot) {
        List<ForecastWindowRecord> tomorrowWindows = snapshot.tomorrowWindows();
        List<String> items = new ArrayList<>();

        ForecastWindowRecord cheapestTomorrow = findCheapest(tomorrowWindows);
        ForecastWindowRecord cleanestTomorrow = findCleanest(tomorrowWindows);
        ForecastWindowRecord peakSolarTomorrow = findPeakSolar(tomorrowWindows);
        ForecastWindowRecord firstBatteryShift = findBatteryAction(tomorrowWindows, "charge", "discharge");

        if (peakSolarTomorrow != null) {
            items.add("Peak solar is forecast around " + formatTime(peakSolarTomorrow.windowStart())
                    + " at " + formatSolar(peakSolarTomorrow.solarShortwaveWm2()) + ".");
        }

        if (cheapestTomorrow != null) {
            items.add("Cheapest import slot is " + formatPrice(cheapestTomorrow.tariffPPerKwh())
                    + " near " + formatTime(cheapestTomorrow.windowStart()) + ".");
        }

        if (cleanestTomorrow != null && !sameWindow(cleanestTomorrow, cheapestTomorrow)) {
            items.add("Cleanest grid window is " + formatCarbon(cleanestTomorrow.carbonGco2PerKwh())
                    + " around " + formatTime(cleanestTomorrow.windowStart()) + ".");
        }

        if (snapshot.homeEnergyAssets().hasBattery() && firstBatteryShift != null) {
            items.add("Battery is forecast to " + normalizeBatteryAction(firstBatteryShift.batteryAction())
                    + " around " + formatTime(firstBatteryShift.windowStart())
                    + " at about " + formatSoc(firstBatteryShift.batterySocPctPred()) + ".");
        }

        if (items.isEmpty() && !tomorrowWindows.isEmpty()) {
            items.add("Tomorrow's forecast cache is live, but the strongest solar and battery signals are still smoothing out.");
        }

        if (items.isEmpty()) {
            items.add("Tomorrow's forecast cache is still refreshing.");
        }

        String headline;
        if (peakSolarTomorrow != null) {
            headline = "Tomorrow looks strongest around " + formatTime(peakSolarTomorrow.windowStart()) + ".";
        } else if (cheapestTomorrow != null) {
            headline = "Tomorrow has at least one cheaper import window to plan around.";
        } else {
            headline = "Tomorrow's forecast is still building.";
        }

        return new TomorrowSummarySnapshot(headline, dedupe(items));
    }

    public BatteryPanelSnapshot buildBatteryPanel(ForecastOverviewSnapshot snapshot) {
        HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets = snapshot.homeEnergyAssets();
        List<ForecastWindowRecord> tomorrowBatteryWindows = snapshot.tomorrowWindows().stream()
                .filter(window -> window.batterySocPctPred() != null)
                .toList();
        List<String> highlights = new ArrayList<>();

        if (!homeEnergyAssets.hasBattery()) {
            highlights.add("No home battery is configured in the current profile.");
            return new BatteryPanelSnapshot(
                    "Battery setup needed",
                    "Add a battery in setup or settings to unlock storage planning here.",
                    highlights
            );
        }

        if (tomorrowBatteryWindows.isEmpty()) {
            highlights.add("Battery state-of-charge predictions have not landed in the forecast cache yet.");
            return new BatteryPanelSnapshot(
                    "Battery forecast is refreshing",
                    "The rest of the forecast is available, but the storage curve is still catching up.",
                    highlights
            );
        }

        ForecastWindowRecord minSocWindow = tomorrowBatteryWindows.stream()
                .min(Comparator.comparingDouble(window -> window.batterySocPctPred() == null ? Double.MAX_VALUE : window.batterySocPctPred()))
                .orElse(null);
        ForecastWindowRecord maxSocWindow = tomorrowBatteryWindows.stream()
                .max(Comparator.comparingDouble(window -> window.batterySocPctPred() == null ? Double.MIN_VALUE : window.batterySocPctPred()))
                .orElse(null);
        ForecastWindowRecord chargeWindow = findBatteryAction(tomorrowBatteryWindows, "charge");
        ForecastWindowRecord dischargeWindow = findBatteryAction(tomorrowBatteryWindows, "discharge");

        if (chargeWindow != null) {
            highlights.add("Charging is forecast around " + formatTime(chargeWindow.windowStart())
                    + " with battery level near " + formatSoc(chargeWindow.batterySocPctPred()) + ".");
        }
        if (dischargeWindow != null) {
            highlights.add("Discharge is forecast around " + formatTime(dischargeWindow.windowStart())
                    + " with battery level near " + formatSoc(dischargeWindow.batterySocPctPred()) + ".");
        }
        if (minSocWindow != null && maxSocWindow != null) {
            highlights.add("Tomorrow's battery range spans " + formatSoc(minSocWindow.batterySocPctPred())
                    + " to " + formatSoc(maxSocWindow.batterySocPctPred()) + ".");
        }
        if (homeEnergyAssets.batteryCapacityKwh() != null) {
            highlights.add("Stored capacity on file: "
                    + String.format(Locale.UK, "%.1f kWh", homeEnergyAssets.batteryCapacityKwh()) + ".");
        }

        String headline = maxSocWindow == null
                ? "Battery outlook for tomorrow"
                : "Battery is forecast to peak near " + formatSoc(maxSocWindow.batterySocPctPred())
                + " around " + formatTime(maxSocWindow.windowStart()) + ".";
        String body = minSocWindow == null
                ? "Battery state-of-charge predictions are available."
                : "The lowest forecast point is " + formatSoc(minSocWindow.batterySocPctPred())
                + " near " + formatTime(minSocWindow.windowStart()) + ".";

        return new BatteryPanelSnapshot(headline, body, dedupe(highlights));
    }

    private ForecastWindowRecord findCheapest(List<ForecastWindowRecord> windows) {
        return windows.stream()
                .filter(window -> window.tariffPPerKwh() != null)
                .min(Comparator.comparingDouble(ForecastWindowRecord::tariffPPerKwh))
                .orElse(null);
    }

    private ForecastWindowRecord findCleanest(List<ForecastWindowRecord> windows) {
        return windows.stream()
                .filter(window -> window.carbonGco2PerKwh() != null)
                .min(Comparator.comparingDouble(ForecastWindowRecord::carbonGco2PerKwh))
                .orElse(null);
    }

    private ForecastWindowRecord findPeakSolar(List<ForecastWindowRecord> windows) {
        return windows.stream()
                .filter(window -> window.solarShortwaveWm2() != null)
                .max(Comparator.comparingDouble(ForecastWindowRecord::solarShortwaveWm2))
                .orElse(null);
    }

    private ForecastWindowRecord findBatteryAction(List<ForecastWindowRecord> windows, String... keywords) {
        List<String> loweredKeywords = new ArrayList<>();
        for (String keyword : keywords) {
            loweredKeywords.add(keyword.toLowerCase(Locale.ROOT));
        }

        return windows.stream()
                .filter(window -> window.batteryAction() != null && !window.batteryAction().isBlank())
                .filter(window -> {
                    String loweredAction = window.batteryAction().toLowerCase(Locale.ROOT);
                    return loweredKeywords.stream().anyMatch(loweredAction::contains);
                })
                .findFirst()
                .orElse(null);
    }

    private boolean sameWindow(ForecastWindowRecord first, ForecastWindowRecord second) {
        if (first == null || second == null) {
            return false;
        }
        return first.windowStart() != null && first.windowStart().equals(second.windowStart());
    }

    private List<String> dedupe(List<String> items) {
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    private String normalizeBatteryAction(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return "change state";
        }

        String lowered = rawAction.trim().toLowerCase(Locale.ROOT);
        if (lowered.contains("discharge")) {
            return "discharge";
        }
        if (lowered.contains("charge")) {
            return "charge";
        }
        return lowered.replace('_', ' ');
    }

    private String formatTime(LocalDateTime timestamp) {
        return timestamp == null ? "later" : timestamp.toLocalTime().format(TIME_FMT);
    }

    private String formatPrice(Double price) {
        return price == null ? "an unknown tariff" : String.format(Locale.UK, "%.1fp/kWh", price);
    }

    private String formatCarbon(Double carbon) {
        return carbon == null ? "unknown carbon intensity" : String.format(Locale.UK, "%.0f gCO2/kWh", carbon);
    }

    private String formatSolar(Double solar) {
        return solar == null ? "unknown solar output" : String.format(Locale.UK, "%.0f W/m2", solar);
    }

    private String formatSoc(Double soc) {
        return soc == null ? "unknown charge" : String.format(Locale.UK, "%.0f%%", soc);
    }

    public record TomorrowSummarySnapshot(
            String headline,
            List<String> items
    ) {
        public TomorrowSummarySnapshot {
            items = List.copyOf(items);
        }
    }

    public record BatteryPanelSnapshot(
            String headline,
            String body,
            List<String> highlights
    ) {
        public BatteryPanelSnapshot {
            highlights = List.copyOf(highlights);
        }
    }

    public record ForecastOverviewSnapshot(
            LocalDateTime anchorTime,
            ZoneId homeZone,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets,
            List<ForecastWindowRecord> upcomingWindows,
            List<ForecastWindowRecord> anchorDayWindows,
            List<ForecastWindowRecord> tomorrowWindows
    ) {
        public List<ForecastWindowRecord> twoDayWindows() {
            List<ForecastWindowRecord> combined = new ArrayList<>(anchorDayWindows);
            combined.addAll(tomorrowWindows);
            return List.copyOf(combined);
        }
    }
}
