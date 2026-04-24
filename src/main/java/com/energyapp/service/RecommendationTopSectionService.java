package com.energyapp.service;

import com.energyapp.db.ForecastWindowRecord;
import com.energyapp.db.HomeEnergyAssetsRepository;
import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RecommendationTopSectionService {

    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final double STRONG_SOLAR_THRESHOLD_WM2 = 250.0;

    private final RecommendationEngine recommendationEngine;
    private final HomeProfileRepository homeProfileRepository;
    private final ForecastOverviewService forecastOverviewService;
    private final Clock clock;

    public RecommendationTopSectionService() {
        this(
                new RecommendationEngine(),
                new HomeProfileRepository(),
                new ForecastOverviewService(),
                Clock.systemDefaultZone()
        );
    }

    RecommendationTopSectionService(
            RecommendationEngine recommendationEngine,
            HomeProfileRepository homeProfileRepository,
            ForecastOverviewService forecastOverviewService,
            Clock clock
    ) {
        this.recommendationEngine = recommendationEngine;
        this.homeProfileRepository = homeProfileRepository;
        this.forecastOverviewService = forecastOverviewService;
        this.clock = clock;
    }

    public TopSectionsSnapshot build(EnergyRecord currentRecord) {
        Double weeklyBudget = homeProfileRepository.getWeeklyBudgetGbp(HOME_ID);
        LocalDateTime referenceTime = resolveReferenceTime(currentRecord);
        ForecastOverviewService.ForecastOverviewSnapshot forecastOverview =
                forecastOverviewService.load(HOME_ID, referenceTime);

        List<Recommendation> activeRecommendations = recommendationEngine.getActiveRecommendations(currentRecord);
        List<ForecastWindowRecord> upcomingWindows = forecastOverview.upcomingWindows();
        List<ForecastWindowRecord> tomorrowWindows = forecastOverview.tomorrowWindows();
        List<ForecastWindowRecord> effectiveTomorrowWindows = tomorrowWindows.isEmpty() ? upcomingWindows : tomorrowWindows;

        return new TopSectionsSnapshot(
                buildPriorityCard(currentRecord, activeRecommendations, upcomingWindows, effectiveTomorrowWindows, forecastOverview.homeEnergyAssets(), weeklyBudget),
                buildTonightSection(currentRecord, upcomingWindows, forecastOverview.homeEnergyAssets()),
                buildTomorrowSection(effectiveTomorrowWindows, forecastOverview.homeEnergyAssets())
        );
    }

    private LocalDateTime resolveReferenceTime(EnergyRecord currentRecord) {
        if (currentRecord != null && currentRecord.getTimestamp() != null) {
            return currentRecord.getTimestamp();
        }
        return LocalDateTime.now(clock);
    }

    private PriorityCard buildPriorityCard(
            EnergyRecord currentRecord,
            List<Recommendation> activeRecommendations,
            List<ForecastWindowRecord> upcomingWindows,
            List<ForecastWindowRecord> tomorrowWindows,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets,
            Double weeklyBudget
    ) {
        ForecastWindowRecord cheapestUpcoming = findCheapest(upcomingWindows);
        ForecastWindowRecord cleanestUpcoming = findCleanest(upcomingWindows);
        ForecastWindowRecord tomorrowPeakSolar = findPeakSolar(tomorrowWindows);
        ForecastWindowRecord upcomingBatteryAction = findBatteryAction(upcomingWindows, "charge", "discharge");
        ForecastWindowRecord tomorrowBatteryAction = findBatteryAction(tomorrowWindows, "charge", "discharge");

        Recommendation priorityRecommendation = activeRecommendations.stream()
                .max(Comparator.comparingInt(this::priorityScore))
                .orElse(null);

        if (priorityRecommendation != null) {
            NavigationTarget navigationTarget = switch (priorityRecommendation.getId()) {
                case "high-heating" -> NavigationTarget.DEVICES;
                default -> NavigationTarget.FORECASTS;
            };

            return new PriorityCard(
                    priorityRecommendation.getTitle(),
                    priorityRecommendation.getExplanation(),
                    buildPriorityWhy(priorityRecommendation, currentRecord, cheapestUpcoming, cleanestUpcoming, tomorrowPeakSolar, upcomingBatteryAction),
                    buildPriorityImpact(priorityRecommendation, weeklyBudget, homeEnergyAssets, tomorrowPeakSolar, tomorrowBatteryAction),
                    navigationTarget == NavigationTarget.DEVICES ? "Review Devices" : "Open Forecasts",
                    navigationTarget
            );
        }

        if (tomorrowPeakSolar != null && homeEnergyAssets.hasSolar()) {
            return new PriorityCard(
                    "Tomorrow has a strong solar window",
                    "Peak solar is forecast around " + formatTime(tomorrowPeakSolar.windowStart())
                            + ", so flexible loads should perform best around midday.",
                    "Why now: solar is expected to reach " + formatSolar(tomorrowPeakSolar.solarShortwaveWm2())
                            + " and your home profile includes a " + formatSolarCapacity(homeEnergyAssets) + " setup.",
                    buildSolarImpact(weeklyBudget, tomorrowBatteryAction),
                    "Open Forecasts",
                    NavigationTarget.FORECASTS
            );
        }

        if (cheapestUpcoming != null) {
            String summary = "The cheapest upcoming import window is "
                    + formatPrice(cheapestUpcoming.tariffPPerKwh())
                    + " around " + formatTime(cheapestUpcoming.windowStart()) + ".";
            return new PriorityCard(
                    "A cheaper import window is coming up",
                    summary,
                    buildForecastFallbackWhy(currentRecord, cleanestUpcoming),
                    weeklyBudget == null
                            ? "Use the forecast page to line up charging, laundry, or heating with the best slot."
                            : "Shifting flexible loads helps protect your " + formatCurrency(weeklyBudget) + " weekly budget.",
                    "Open Forecasts",
                    NavigationTarget.FORECASTS
            );
        }

        return new PriorityCard(
                "Your home looks steady right now",
                "No urgent live recommendation is active at the moment.",
                buildSteadyStateWhy(currentRecord),
                weeklyBudget == null
                        ? "Forecast feeds are still useful for planning the next cheap or clean window."
                        : "Your home profile already has a weekly budget of " + formatCurrency(weeklyBudget) + ".",
                "Open Forecasts",
                NavigationTarget.FORECASTS
        );
    }

    private String buildPriorityWhy(
            Recommendation recommendation,
            EnergyRecord currentRecord,
            ForecastWindowRecord cheapestUpcoming,
            ForecastWindowRecord cleanestUpcoming,
            ForecastWindowRecord tomorrowPeakSolar,
            ForecastWindowRecord upcomingBatteryAction
    ) {
        return switch (recommendation.getId()) {
            case "high-price" -> currentRecord == null
                    ? "Why now: forecasted tariffs are elevated."
                    : "Why now: import is " + formatPrice(currentRecord.getPricePerKwh())
                    + " right now, while the next cheaper slot is "
                    + formatPrice(cheapestUpcoming == null ? null : cheapestUpcoming.tariffPPerKwh())
                    + " at " + formatTime(cheapestUpcoming == null ? null : cheapestUpcoming.windowStart()) + ".";
            case "high-carbon" -> currentRecord == null
                    ? "Why now: grid carbon is elevated."
                    : "Why now: grid carbon is " + formatCarbon(currentRecord.getCarbon())
                    + ", with a cleaner slot at " + formatTime(cleanestUpcoming == null ? null : cleanestUpcoming.windowStart())
                    + " forecast around " + formatCarbon(cleanestUpcoming == null ? null : cleanestUpcoming.carbonGco2PerKwh()) + ".";
            case "low-battery" -> "Why now: battery charge is at " + formatBatteryLevel(currentRecord)
                    + batteryForecastSuffix(upcomingBatteryAction) + ".";
            case "high-heating" -> currentRecord == null
                    ? "Why now: heating demand is elevated."
                    : "Why now: heating used " + formatKwh(currentRecord.getHeatingUsageKwh())
                    + " this interval, so the next cheaper tariff matters more than usual.";
            case "high-solar" -> currentRecord == null
                    ? "Why now: solar production is strong."
                    : "Why now: solar is already at " + formatKwh(currentRecord.getSolarGenKwh())
                    + " and tomorrow peaks near " + formatTime(tomorrowPeakSolar == null ? null : tomorrowPeakSolar.windowStart()) + ".";
            default -> "Why now: live data shows a worthwhile opportunity.";
        };
    }

    private String buildPriorityImpact(
            Recommendation recommendation,
            Double weeklyBudget,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets,
            ForecastWindowRecord tomorrowPeakSolar,
            ForecastWindowRecord tomorrowBatteryAction
    ) {
        if ("high-solar".equals(recommendation.getId()) && homeEnergyAssets.hasSolar()) {
            return buildSolarImpact(weeklyBudget, tomorrowBatteryAction)
                    + " Peak solar tomorrow is forecast at "
                    + formatSolar(tomorrowPeakSolar == null ? null : tomorrowPeakSolar.solarShortwaveWm2()) + ".";
        }

        if ("low-battery".equals(recommendation.getId()) && tomorrowBatteryAction != null) {
            return "Forecast cache suggests the battery may "
                    + normalizeBatteryAction(tomorrowBatteryAction.batteryAction())
                    + " near " + formatTime(tomorrowBatteryAction.windowStart())
                    + " at about " + formatSoc(tomorrowBatteryAction.batterySocPctPred()) + ".";
        }

        if (weeklyBudget != null) {
            return "Acting on this now helps protect your " + formatCurrency(weeklyBudget) + " weekly budget.";
        }

        if (homeEnergyAssets.hasBattery()) {
            return "Your profile shows a home battery, so time-shifting should be especially useful.";
        }

        return "Use the forecast view to line this up with the cheapest or cleanest upcoming window.";
    }

    private String buildForecastFallbackWhy(EnergyRecord currentRecord, ForecastWindowRecord cleanestUpcoming) {
        if (currentRecord == null) {
            return "Why now: forecast data has identified a more efficient import window.";
        }

        if (cleanestUpcoming != null) {
            return "Why now: live import is " + formatPrice(currentRecord.getPricePerKwh())
                    + " and the cleanest nearby slot is " + formatCarbon(cleanestUpcoming.carbonGco2PerKwh())
                    + " around " + formatTime(cleanestUpcoming.windowStart()) + ".";
        }

        return "Why now: live import is " + formatPrice(currentRecord.getPricePerKwh())
                + " and upcoming forecast data suggests a lower-cost window.";
    }

    private String buildSteadyStateWhy(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            return "Why now: live simulation data has not arrived yet.";
        }

        return "Why now: live import is " + formatPrice(currentRecord.getPricePerKwh())
                + ", grid carbon is " + formatCarbon(currentRecord.getCarbon())
                + ", and battery charge is " + formatBatteryLevel(currentRecord) + ".";
    }

    private Section buildTonightSection(
            EnergyRecord currentRecord,
            List<ForecastWindowRecord> upcomingWindows,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets
    ) {
        List<String> items = new ArrayList<>();
        ForecastWindowRecord cheapestUpcoming = findCheapest(upcomingWindows);
        ForecastWindowRecord cleanestUpcoming = findCleanest(upcomingWindows);
        ForecastWindowRecord batteryActionWindow = findBatteryAction(upcomingWindows, "charge", "discharge");

        if (cheapestUpcoming != null) {
            items.add("Cheapest upcoming tariff is " + formatPrice(cheapestUpcoming.tariffPPerKwh())
                    + " at " + formatTime(cheapestUpcoming.windowStart()) + ".");
        }

        if (cleanestUpcoming != null && !sameWindow(cheapestUpcoming, cleanestUpcoming)) {
            items.add("Cleanest upcoming slot is " + formatCarbon(cleanestUpcoming.carbonGco2PerKwh())
                    + " at " + formatTime(cleanestUpcoming.windowStart()) + ".");
        }

        if (homeEnergyAssets.hasBattery() && batteryActionWindow != null) {
            items.add("Battery is forecast to " + normalizeBatteryAction(batteryActionWindow.batteryAction())
                    + " around " + formatTime(batteryActionWindow.windowStart())
                    + " at about " + formatSoc(batteryActionWindow.batterySocPctPred()) + ".");
        }

        if (items.isEmpty() && currentRecord != null) {
            items.add("Live import is " + formatPrice(currentRecord.getPricePerKwh())
                    + " at " + formatTime(currentRecord.getTimestamp()) + ".");
            items.add("Grid carbon is " + formatCarbon(currentRecord.getCarbon()) + " right now.");
        }

        if (items.isEmpty()) {
            items.add("Tariff and carbon forecasts are still refreshing from the live cache.");
        }

        return new Section(dedupe(items));
    }

    private Section buildTomorrowSection(
            List<ForecastWindowRecord> tomorrowWindows,
            HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets
    ) {
        List<String> items = new ArrayList<>();
        ForecastWindowRecord peakSolarWindow = findPeakSolar(tomorrowWindows);
        ForecastWindowRecord strongSolarWindow = findFirstStrongSolar(tomorrowWindows);
        ForecastWindowRecord batteryChargeWindow = findBatteryAction(tomorrowWindows, "charge");

        if (peakSolarWindow != null) {
            items.add("Peak solar is forecast around " + formatTime(peakSolarWindow.windowStart())
                    + " at " + formatSolar(peakSolarWindow.solarShortwaveWm2()) + ".");
        }

        if (strongSolarWindow != null && !sameWindow(peakSolarWindow, strongSolarWindow)) {
            items.add("Best self-use window begins near " + formatTime(strongSolarWindow.windowStart())
                    + " when solar moves above " + formatSolar(STRONG_SOLAR_THRESHOLD_WM2) + ".");
        }

        if (homeEnergyAssets.hasBattery() && batteryChargeWindow != null) {
            items.add("Battery is forecast to charge around " + formatTime(batteryChargeWindow.windowStart())
                    + " and could reach about " + formatSoc(batteryChargeWindow.batterySocPctPred()) + ".");
        }

        if (items.isEmpty() && homeEnergyAssets.hasSolar()) {
            items.add("Tomorrow's solar forecast is still refreshing for your " + formatSolarCapacity(homeEnergyAssets) + " setup.");
        }

        if (items.isEmpty() && !homeEnergyAssets.hasSolar()) {
            items.add("No solar asset is configured for this home profile yet.");
        }

        return new Section(dedupe(items));
    }

    private String buildSolarImpact(Double weeklyBudget, ForecastWindowRecord tomorrowBatteryAction) {
        if (tomorrowBatteryAction != null) {
            return "Battery support is forecast around " + formatTime(tomorrowBatteryAction.windowStart())
                    + ", when charge could reach about " + formatSoc(tomorrowBatteryAction.batterySocPctPred()) + ".";
        }

        if (weeklyBudget != null) {
            return "Midday solar shifting can help preserve your " + formatCurrency(weeklyBudget) + " weekly budget.";
        }

        return "This is a good candidate window for laundry, EV charging, or topping up the home battery.";
    }

    private int priorityScore(Recommendation recommendation) {
        return switch (recommendation.getId()) {
            case "high-price" -> 100;
            case "high-carbon" -> 90;
            case "low-battery" -> 80;
            case "high-heating" -> 70;
            case "high-solar" -> 60;
            default -> 10;
        };
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

    private ForecastWindowRecord findFirstStrongSolar(List<ForecastWindowRecord> windows) {
        return windows.stream()
                .filter(window -> window.solarShortwaveWm2() != null && window.solarShortwaveWm2() >= STRONG_SOLAR_THRESHOLD_WM2)
                .findFirst()
                .orElse(null);
    }

    private ForecastWindowRecord findBatteryAction(List<ForecastWindowRecord> windows, String... keywords) {
        Set<String> loweredKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            loweredKeywords.add(keyword.toLowerCase(Locale.ROOT));
        }

        return windows.stream()
                .filter(window -> window.batteryAction() != null && !window.batteryAction().isBlank())
                .filter(window -> {
                    String action = window.batteryAction().toLowerCase(Locale.ROOT);
                    return loweredKeywords.stream().anyMatch(action::contains);
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

    private String batteryForecastSuffix(ForecastWindowRecord window) {
        if (window == null) {
            return "";
        }
        return " and forecast to " + normalizeBatteryAction(window.batteryAction())
                + " around " + formatTime(window.windowStart()) + ".";
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

    private String formatKwh(double kwh) {
        return String.format(Locale.UK, "%.1f kWh", kwh);
    }

    private String formatSoc(Double soc) {
        return soc == null ? "unknown charge" : String.format(Locale.UK, "%.0f%%", soc);
    }

    private String formatBatteryLevel(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            return "unknown";
        }
        return currentRecord.getBatteryLevel() + "%";
    }

    private String formatCurrency(Double amount) {
        return amount == null ? "no budget" : String.format(Locale.UK, "\u00A3%.2f", amount);
    }

    private String formatSolarCapacity(HomeEnergyAssetsRepository.HomeEnergyAssets homeEnergyAssets) {
        return String.format(Locale.UK, "%.1f kW", homeEnergyAssets.solarCapacityOrDefault(4.0));
    }

    public enum NavigationTarget {
        FORECASTS,
        DEVICES
    }

    public record PriorityCard(
            String title,
            String summary,
            String why,
            String impact,
            String actionLabel,
            NavigationTarget navigationTarget
    ) {
    }

    public record Section(List<String> items) {
        public Section {
            items = List.copyOf(items);
        }
    }

    public record TopSectionsSnapshot(
            PriorityCard priorityCard,
            Section tonightSection,
            Section tomorrowSection
    ) {
    }
}
