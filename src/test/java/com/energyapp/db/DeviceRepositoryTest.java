package com.energyapp.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceRepositoryTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private DeviceRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO home_profile(home_id, first_name, timezone, created_at) VALUES(?,?,?,datetime('now'))")) {
                stmt.setString(1, "HOME_1");
                stmt.setString(2, "Richard");
                stmt.setString(3, "Europe/London");
                stmt.executeUpdate();
            }
        }

        repository = new DeviceRepository();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void insertAndFetchActiveDevices_persistsDirectAndScheduledRows() {
        repository.insert(sampleRecord("DEV_UI_1", "Office Plug", "SMART_PLUG", "SMART_PLUG").value());
        repository.insert(sampleRecord("DEV_UI_2", "Laundry", "WASHING_MACHINE", "LAUNDRY")
                .withSchedule("07:30")
                .value());

        List<CanonicalDeviceRecord> devices = repository.fetchActiveByHome("HOME_1");

        assertEquals(2, devices.size());
        assertEquals("Laundry", devices.get(0).displayName());
        assertEquals("Office Plug", devices.get(1).displayName());

        CanonicalDeviceRecord scheduled = repository.findById("DEV_UI_2").orElseThrow();
        assertEquals("WASHING_MACHINE", scheduled.deviceType());
        assertEquals("07:30", scheduled.scheduleTime());
        assertEquals("LAUNDRY", scheduled.category());
    }

    @Test
    void update_persistsStateScheduleAndRuntimeFields() {
        repository.insert(sampleRecord("DEV_UI_3", "Dishwasher", "DISHWASHER", "KITCHEN").value());

        CanonicalDeviceRecord updated = sampleRecord("DEV_UI_3", "Dishwasher", "DISHWASHER", "KITCHEN")
                .withPowerState("ON")
                .withStatus("RUNNING")
                .withSchedule("21:15")
                .withPriceCutoff(24.5)
                .withActiveRunUntil("2026-04-23 22:15:00")
                .withRunMode("MANUAL")
                .withLastRunStartedAt("2026-04-23 21:15:00")
                .withLastRunEndedAt("2026-04-23 22:15:00")
                .withLastScheduledRunDate("2026-04-23")
                .withSkipNextRun(true)
                .withLocationLabel("Kitchen")
                .withUpdatedAt("2026-04-23 21:16:00")
                .value();
        repository.update(updated);

        CanonicalDeviceRecord persisted = repository.findById("DEV_UI_3").orElseThrow();
        assertEquals("ON", persisted.powerState());
        assertEquals("RUNNING", persisted.status());
        assertEquals("21:15", persisted.scheduleTime());
        assertEquals(24.5, persisted.priceCutoffPPerKwh(), 0.0001);
        assertEquals("2026-04-23 22:15:00", persisted.activeRunUntil());
        assertEquals("MANUAL", persisted.runMode());
        assertEquals("2026-04-23 21:15:00", persisted.lastRunStartedAt());
        assertEquals("2026-04-23 22:15:00", persisted.lastRunEndedAt());
        assertEquals("2026-04-23", persisted.lastScheduledRunDate());
        assertTrue(persisted.skipNextRun());
        assertEquals("Kitchen", persisted.locationLabel());
        assertEquals("2026-04-23 21:16:00", persisted.updatedAt());
    }

    @Test
    void softDelete_hidesRowFromActiveFetchButKeepsStoredRecord() {
        repository.insert(sampleRecord("DEV_UI_4", "Bedroom Plug", "SMART_PLUG", "SMART_PLUG").value());

        repository.softDelete("DEV_UI_4", Instant.parse("2026-04-23T10:15:30Z"));

        assertTrue(repository.fetchActiveByHome("HOME_1").isEmpty());
        CanonicalDeviceRecord deleted = repository.findById("DEV_UI_4").orElseThrow();
        assertEquals("2026-04-23 10:15:30", deleted.deletedAt());
    }

    @Test
    void dataPersistsAcrossRepositoryReopen() {
        repository.insert(sampleRecord("DEV_UI_5", "Hall Fridge", "FRIDGE", "KITCHEN")
                .withPowerState("OFF")
                .withStatus("OFF")
                .value());

        DeviceRepository reopenedRepository = new DeviceRepository();
        CanonicalDeviceRecord persisted = reopenedRepository.findById("DEV_UI_5").orElseThrow();

        assertEquals("Hall Fridge", persisted.displayName());
        assertEquals("FRIDGE", persisted.deviceType());
        assertFalse(persisted.greenEnergyOnly());
    }

    private CanonicalDeviceFixture sampleRecord(String deviceId, String displayName, String deviceType, String category) {
        return new CanonicalDeviceFixture(new CanonicalDeviceRecord(
                deviceId,
                "HOME_1",
                displayName,
                deviceType,
                category,
                null,
                null,
                null,
                null,
                "OFF",
                "IDLE",
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                "2026-04-23 08:00:00",
                "2026-04-23 08:00:00",
                null
        ));
    }

    private record CanonicalDeviceFixture(CanonicalDeviceRecord value) {
        private CanonicalDeviceFixture withSchedule(String scheduleTime) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), scheduleTime, value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withPowerState(String powerState) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    powerState, value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withStatus(String status) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), status, value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withPriceCutoff(Double priceCutoff) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), priceCutoff, value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withActiveRunUntil(String activeRunUntil) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    activeRunUntil, value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withRunMode(String runMode) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), runMode, value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withLastRunStartedAt(String lastRunStartedAt) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), lastRunStartedAt, value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withLastRunEndedAt(String lastRunEndedAt) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), lastRunEndedAt,
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withLastScheduledRunDate(String lastScheduledRunDate) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    lastScheduledRunDate, value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withSkipNextRun(boolean skipNextRun) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), skipNextRun, value.locationLabel(),
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withLocationLabel(String locationLabel) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), locationLabel,
                    value.createdAt(), value.updatedAt(), value.deletedAt()
            ));
        }

        private CanonicalDeviceFixture withUpdatedAt(String updatedAt) {
            return new CanonicalDeviceFixture(copy(
                    value.deviceId(), value.homeId(), value.displayName(), value.deviceType(), value.category(),
                    value.iconKey(), value.typicalPowerW(), value.typicalCycleKwh(), value.typicalDurationMins(),
                    value.powerState(), value.status(), value.currentDrawW(), value.greenEnergyOnly(),
                    value.readyByTime(), value.scheduleTime(), value.priceCutoffPPerKwh(), value.policy(),
                    value.nextRunWindowStart(), value.nextRunWindowEnd(), value.nextRunEstCostGbp(),
                    value.nextRunEstCo2Kg(), value.nextRunSourceSolarPct(), value.nextRunSourceGridPct(),
                    value.activeRunUntil(), value.runMode(), value.lastRunStartedAt(), value.lastRunEndedAt(),
                    value.lastScheduledRunDate(), value.skipNextRun(), value.locationLabel(),
                    value.createdAt(), updatedAt, value.deletedAt()
            ));
        }

        private static CanonicalDeviceRecord copy(
                String deviceId,
                String homeId,
                String displayName,
                String deviceType,
                String category,
                String iconKey,
                Double typicalPowerW,
                Double typicalCycleKwh,
                Integer typicalDurationMins,
                String powerState,
                String status,
                Double currentDrawW,
                boolean greenEnergyOnly,
                String readyByTime,
                String scheduleTime,
                Double priceCutoffPPerKwh,
                String policy,
                String nextRunWindowStart,
                String nextRunWindowEnd,
                Double nextRunEstCostGbp,
                Double nextRunEstCo2Kg,
                Double nextRunSourceSolarPct,
                Double nextRunSourceGridPct,
                String activeRunUntil,
                String runMode,
                String lastRunStartedAt,
                String lastRunEndedAt,
                String lastScheduledRunDate,
                boolean skipNextRun,
                String locationLabel,
                String createdAt,
                String updatedAt,
                String deletedAt
        ) {
            return new CanonicalDeviceRecord(
                    deviceId, homeId, displayName, deviceType, category, iconKey,
                    typicalPowerW, typicalCycleKwh, typicalDurationMins,
                    powerState, status, currentDrawW, greenEnergyOnly,
                    readyByTime, scheduleTime, priceCutoffPPerKwh, policy,
                    nextRunWindowStart, nextRunWindowEnd, nextRunEstCostGbp,
                    nextRunEstCo2Kg, nextRunSourceSolarPct, nextRunSourceGridPct,
                    activeRunUntil, runMode, lastRunStartedAt, lastRunEndedAt,
                    lastScheduledRunDate, skipNextRun, locationLabel,
                    createdAt, updatedAt, deletedAt
            );
        }
    }
}
