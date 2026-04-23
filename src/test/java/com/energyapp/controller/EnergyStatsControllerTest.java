package com.energyapp.controller;

import com.energyapp.db.HomeEnergyAssetsRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyStatsControllerTest {

    private static final HomeEnergyAssetsRepository.HomeEnergyAssets ASSETS =
            new HomeEnergyAssetsRepository.HomeEnergyAssets(true, true, 10.5, 20, 5.0, 4.0);

    @Test
    void buildSummarySnapshot_usesLatestRecordDateInsteadOfSystemDate() {
        LocalDateTime previousDay = LocalDateTime.of(2026, 1, 4, 23, 30);
        LocalDateTime latestDay = LocalDateTime.of(2026, 1, 5, 0, 0);

        EnergyStatsController.SummarySnapshot summary = EnergyStatsController.buildSummarySnapshot(List.of(
                record(previousDay, 100.0, 10.0, 2.0, 0.0, 50),
                record(latestDay, 3.0, 20.0, 1.0, 2.0, 42),
                record(latestDay.plusMinutes(30), 5.0, 30.0, 2.0, 3.0, 43)
        ), ASSETS);

        assertEquals(latestDay.plusMinutes(30), summary.latestRecord().getTimestamp());
        assertEquals(8.0, summary.totalEnergyToday(), 0.0001);
        assertEquals(3.0, summary.totalSolarToday(), 0.0001);
        assertEquals(1.30, summary.totalCostToday(), 0.0001);
    }

    @Test
    void calculateSourceBreakdown_estimatesSolarBatteryAndGridContribution() {
        EnergyStatsController.SourceBreakdown breakdown = EnergyStatsController.calculateSourceBreakdown(List.of(
                record(LocalDateTime.of(2026, 1, 5, 10, 0), 10.0, 20.0, 3.0, 4.0, 50)
        ));

        assertEquals(3.0, breakdown.solarKwh(), 0.0001);
        assertEquals(3.0, breakdown.batteryKwh(), 0.0001);
        assertEquals(4.0, breakdown.gridKwh(), 0.0001);
        assertEquals(30, breakdown.solarPercentDisplay());
        assertEquals(30, breakdown.batteryPercentDisplay());
        assertEquals(40, breakdown.gridPercentDisplay());
    }

    @Test
    void calculateSourceBreakdown_handlesZeroLoadWithoutDivisionByZero() {
        EnergyStatsController.SourceBreakdown breakdown = EnergyStatsController.calculateSourceBreakdown(List.of(
                record(LocalDateTime.of(2026, 1, 5, 10, 0), 0.0, 20.0, 5.0, 0.0, 50)
        ));

        assertEquals(0, breakdown.solarPercentDisplay());
        assertEquals(0, breakdown.batteryPercentDisplay());
        assertEquals(0, breakdown.gridPercentDisplay());
    }

    @Test
    void calculateSourceBreakdown_clampsDisplayedPercentages() {
        EnergyStatsController.SourceBreakdown breakdown = EnergyStatsController.calculateSourceBreakdown(List.of(
                record(LocalDateTime.of(2026, 1, 5, 10, 0), 1.0, 20.0, 99.0, 0.0, 50)
        ));

        assertEquals(100, breakdown.solarPercentDisplay());
        assertEquals(0, breakdown.batteryPercentDisplay());
        assertEquals(0, breakdown.gridPercentDisplay());
    }

    @Test
    void usageRanges_includeExpectedLabelsAndDefault() {
        assertEquals(EnergyStatsController.UsageRange.LAST_8_HOURS,
                EnergyStatsController.UsageRange.fromLabel("Last 8 hours"));
        assertEquals(EnergyStatsController.UsageRange.REAL_TIME_1_HOUR,
                EnergyStatsController.UsageRange.fromLabel("Real time / 1 hour"));
        assertEquals(EnergyStatsController.UsageRange.LAST_24_HOURS,
                EnergyStatsController.UsageRange.fromLabel("Last 24 hours"));
        assertEquals(EnergyStatsController.UsageRange.LAST_WEEK,
                EnergyStatsController.UsageRange.fromLabel("Last week"));
        assertEquals(EnergyStatsController.UsageRange.LAST_MONTH,
                EnergyStatsController.UsageRange.fromLabel("Last month"));
        assertEquals(EnergyStatsController.UsageRange.LAST_3_MONTHS,
                EnergyStatsController.UsageRange.fromLabel("Last 3 months"));
        assertEquals(EnergyStatsController.UsageRange.ALL_TIME,
                EnergyStatsController.UsageRange.fromLabel("All time"));
        assertEquals(EnergyStatsController.UsageRange.LAST_8_HOURS,
                EnergyStatsController.UsageRange.fromLabel("Unknown"));
    }

    @Test
    void calculateAxisWindow_defaultsToRollingEightHoursWhenAvailable() {
        LocalDateTime latest = LocalDateTime.of(2026, 1, 5, 12, 0);
        EnergyStatsController.AxisWindow window = EnergyStatsController.calculateAxisWindow(List.of(
                record(latest.minusHours(10), 1, 20, 0, 1, 50),
                record(latest, 1, 20, 0, 1, 50)
        ), EnergyStatsController.UsageRange.LAST_8_HOURS);

        assertEquals(DateTimeUtils.toEpochSeconds(latest.minusHours(8)), window.startEpochSec());
        assertEquals(DateTimeUtils.toEpochSeconds(latest), window.endEpochSec());
    }

    @Test
    void chooseTickUnitSeconds_increasesAsVisibleRangeGrows() {
        long oneHourTick = EnergyStatsController.chooseTickUnitSeconds(60L * 60L);
        long eightHourTick = EnergyStatsController.chooseTickUnitSeconds(8L * 60L * 60L);
        long weekTick = EnergyStatsController.chooseTickUnitSeconds(7L * 24L * 60L * 60L);
        long threeMonthTick = EnergyStatsController.chooseTickUnitSeconds(90L * 24L * 60L * 60L);

        assertEquals(10L * 60L, oneHourTick);
        assertEquals(60L * 60L, eightHourTick);
        assertEquals(24L * 60L * 60L, weekTick);
        assertEquals(14L * 24L * 60L * 60L, threeMonthTick);
    }

    private static EnergyRecord record(LocalDateTime timestamp, double totalLoad, double price, double solar, double grid, int battery) {
        return new EnergyRecord(timestamp, timestamp.toLocalTime().toString(), grid, price,
                100.0, totalLoad, solar, battery, 0.0, 0.0, 0.0, 0.0);
    }
}
