package com.energyapp.service;

import com.energyapp.db.CanonicalDeviceRecord;
import com.energyapp.db.DbPaths;
import com.energyapp.db.DeviceRepository;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.Device;
import com.energyapp.model.DeviceRunMode;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.DeviceType;
import com.energyapp.model.EVCharger;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Fridge;
import com.energyapp.model.HeatingDevice;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DeviceServiceTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private SimulationService simService;
    private DeviceService deviceService;
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
        simService = new SimulationService();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void loadsCanonicalDevicesAsConcreteSubclasses() {
        repository.insert(sampleRecord("DEV_SMART", "Desk Plug", "SMART_PLUG", "SMART_PLUG", "ON", "IDLE").value());
        repository.insert(sampleRecord("DEV_WASH", "Laundry", "WASHING_MACHINE", "LAUNDRY", "OFF", "SCHEDULED")
                .withSchedule("07:30")
                .value());
        repository.insert(sampleRecord("DEV_HEAT", "Heating", "HEATING", "HEATING", "OFF", "OFF").value());
        repository.insert(sampleRecord("DEV_EV", "EV Charger", "EV_CHARGER", "EV", "OFF", "OFF").value());
        repository.insert(new CanonicalDeviceFixture(new CanonicalDeviceRecord(
                "DEV_BASE",
                "HOME_1",
                "Baseload",
                null,
                "BASELOAD",
                "base",
                300.0,
                0.8,
                999,
                "ON",
                "IDLE",
                0.0,
                false,
                null,
                null,
                null,
                "BALANCED",
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
        )).value());

        deviceService = new DeviceService(simService, repository, "HOME_1");

        List<Device> devices = deviceService.getAllDevices();
        assertEquals(4, devices.size());
        assertTrue(devices.stream().anyMatch(SmartPlug.class::isInstance));
        assertTrue(devices.stream().anyMatch(WashingMachine.class::isInstance));
        assertTrue(devices.stream().anyMatch(HeatingDevice.class::isInstance));
        assertTrue(devices.stream().anyMatch(EVCharger.class::isInstance));
        assertTrue(devices.stream().noneMatch(device -> "Baseload".equals(device.getDeviceName())));
    }

    @Test
    void addDevice_persistsThroughCanonicalRepository() {
        deviceService = new DeviceService(simService, repository, "HOME_1");

        Fridge fridge = new Fridge("Garage Fridge");
        deviceService.addDevice(fridge);

        assertNotNull(fridge.getDeviceId());
        assertEquals(1, repository.fetchActiveByHome("HOME_1").size());
        CanonicalDeviceRecord persisted = repository.findById(fridge.getDeviceId()).orElseThrow();
        assertEquals("FRIDGE", persisted.deviceType());
        assertEquals("Garage Fridge", persisted.displayName());
    }

    @Test
    void smartPlugCutoff_turnsOffAndPersistsWhenPriceSpikes() {
        repository.insert(sampleRecord("DEV_PLUG", "Heater", "SMART_PLUG", "SMART_PLUG", "ON", "IDLE")
                .withPriceCutoff(30.0)
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        SmartPlug testPlug = (SmartPlug) deviceService.getAllDevices().stream()
                .filter(device -> "Heater".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 12, 0), 80.0);

        assertFalse(testPlug.isOn());
        assertEquals(DeviceStatus.OFF, testPlug.getStatus());
        CanonicalDeviceRecord persisted = repository.findById("DEV_PLUG").orElseThrow();
        assertEquals("OFF", persisted.powerState());
        assertEquals("OFF", persisted.status());
    }

    @Test
    void scheduledDevice_startsAtScheduledTimeStopsAfterTwoHoursAndDoesNotRetriggerSameDay() {
        repository.insert(sampleRecord("DEV_WASH", "Laundry", "WASHING_MACHINE", "LAUNDRY", "OFF", "SCHEDULED")
                .withSchedule("12:00")
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        WashingMachine washingMachine = (WashingMachine) deviceService.getAllDevices().stream()
                .filter(device -> "Laundry".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 11, 30), 25.0);
        assertFalse(washingMachine.isOn());
        assertEquals(DeviceStatus.SCHEDULED, washingMachine.getStatus());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 12, 0), 25.0);
        assertTrue(washingMachine.isOn());
        assertEquals(DeviceStatus.RUNNING, washingMachine.getStatus());
        assertEquals(DeviceRunMode.SCHEDULED, washingMachine.getRunMode());
        assertEquals(LocalDateTime.of(2026, 4, 23, 14, 0), washingMachine.getActiveRunUntil());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 14, 0), 25.0);
        assertFalse(washingMachine.isOn());
        assertEquals(DeviceStatus.OFF, washingMachine.getStatus());
        assertNull(washingMachine.getActiveRunUntil());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 18, 0), 25.0);
        assertFalse(washingMachine.isOn());
        assertEquals(DeviceStatus.OFF, washingMachine.getStatus());

        CanonicalDeviceRecord persisted = repository.findById("DEV_WASH").orElseThrow();
        assertEquals("2026-04-23", persisted.lastScheduledRunDate());
        assertEquals("2026-04-23 14:00:00", persisted.lastRunEndedAt());
        assertEquals("OFF", persisted.powerState());
    }

    @Test
    void skipPreventsNextScheduledRunOnly() {
        repository.insert(sampleRecord("DEV_DISH", "Dishwasher", "DISHWASHER", "KITCHEN", "OFF", "SCHEDULED")
                .withSchedule("19:00")
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        ScheduledDevice dishwasher = (ScheduledDevice) deviceService.getAllDevices().stream()
                .filter(device -> "Dishwasher".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        deviceService.skipNextScheduledRun(dishwasher);
        assertTrue(dishwasher.isSkipNextRun());
        assertEquals(DeviceStatus.SKIPPED, dishwasher.getStatus());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 19, 0), 22.0);
        assertFalse(dishwasher.isOn());
        assertFalse(dishwasher.isSkipNextRun());
        assertEquals(DeviceStatus.SKIPPED, dishwasher.getStatus());
        assertEquals(LocalDate.of(2026, 4, 23), dishwasher.getLastScheduledRunDate());

        setCurrentRecord(LocalDateTime.of(2026, 4, 24, 18, 0), 22.0);
        assertEquals(DeviceStatus.SCHEDULED, dishwasher.getStatus());
    }

    @Test
    void useNowStartsImmediateRunAndPreventsLaterScheduledRetriggerThatDay() {
        repository.insert(sampleRecord("DEV_EV", "EV Charger", "EV_CHARGER", "EV", "OFF", "SCHEDULED")
                .withSchedule("18:30")
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        ScheduledDevice evCharger = (ScheduledDevice) deviceService.getAllDevices().stream()
                .filter(device -> "EV Charger".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        deviceService.useNow(evCharger, LocalDateTime.of(2026, 4, 23, 10, 15));

        assertTrue(evCharger.isOn());
        assertEquals(DeviceStatus.RUNNING, evCharger.getStatus());
        assertEquals(DeviceRunMode.MANUAL, evCharger.getRunMode());
        assertEquals(LocalDateTime.of(2026, 4, 23, 14, 15), evCharger.getActiveRunUntil());
        assertEquals(LocalDate.of(2026, 4, 23), evCharger.getLastScheduledRunDate());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 14, 15), 16.0);
        assertFalse(evCharger.isOn());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 18, 30), 16.0);
        assertFalse(evCharger.isOn());
        assertEquals(DeviceStatus.OFF, evCharger.getStatus());
    }

    @Test
    void rescheduleChangesNextRunAndPersistsSchedule() {
        repository.insert(sampleRecord("DEV_WASH_2", "Laundry", "WASHING_MACHINE", "LAUNDRY", "OFF", "SCHEDULED")
                .withSchedule("08:00")
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        ScheduledDevice washingMachine = (ScheduledDevice) deviceService.getAllDevices().stream()
                .filter(device -> "Laundry".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        deviceService.reschedule(washingMachine, LocalTime.of(16, 30));
        assertEquals(LocalTime.of(16, 30), washingMachine.getSchedule());
        assertEquals(DeviceStatus.SCHEDULED, washingMachine.getStatus());

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 16, 30), 18.0);
        assertTrue(washingMachine.isOn());
        assertEquals(DeviceStatus.RUNNING, washingMachine.getStatus());

        CanonicalDeviceRecord persisted = repository.findById("DEV_WASH_2").orElseThrow();
        assertEquals("16:30", persisted.scheduleTime());
    }

    @Test
    void dishwasherScheduledRunLastsTwoHours() {
        repository.insert(sampleRecord("DEV_DISH_2", "Dishwasher", "DISHWASHER", "KITCHEN", "OFF", "SCHEDULED")
                .withSchedule("18:00")
                .value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        ScheduledDevice dishwasher = (ScheduledDevice) deviceService.getAllDevices().stream()
                .filter(device -> "Dishwasher".equals(device.getDeviceName()))
                .findFirst()
                .orElseThrow();

        setCurrentRecord(LocalDateTime.of(2026, 4, 23, 18, 0), 18.0);
        assertEquals(LocalDateTime.of(2026, 4, 23, 20, 0), dishwasher.getActiveRunUntil());
    }

    @Test
    void washingMachineCannotBeTurnedOffMidCycle() {
        deviceService = new DeviceService(simService, repository, "HOME_1");

        WashingMachine washingMachine = new WashingMachine("Laundry");
        washingMachine.setOn(true);
        washingMachine.setStatus(DeviceStatus.RUNNING);
        washingMachine.setActiveRunUntil(LocalDateTime.now().plusMinutes(30));
        deviceService.addDevice(washingMachine);

        washingMachine.setOn(false);
        deviceService.saveDevice(washingMachine);

        assertTrue(washingMachine.isOn());
        assertEquals(DeviceStatus.RUNNING, washingMachine.getStatus());
    }

    @Test
    void useNowDoesNotRetriggerRunningWashingMachine() {
        deviceService = new DeviceService(simService, repository, "HOME_1");

        WashingMachine washingMachine = new WashingMachine("Laundry");
        washingMachine.setOn(true);
        washingMachine.setStatus(DeviceStatus.RUNNING);
        LocalDateTime activeUntil = LocalDateTime.now().plusHours(1);
        washingMachine.setActiveRunUntil(activeUntil);
        deviceService.addDevice(washingMachine);

        deviceService.useNow(washingMachine, LocalDateTime.now());

        assertEquals(activeUntil, washingMachine.getActiveRunUntil());
    }

    @Test
    void applyPlanPersistsSelectedSchedule() {
        repository.insert(sampleRecord("DEV_WASH_PLAN", "Laundry", "WASHING_MACHINE", "LAUNDRY", "OFF", "OFF").value());
        deviceService = new DeviceService(simService, repository, "HOME_1");

        WashingMachine washingMachine = (WashingMachine) deviceService.getAllDevices().stream()
                .filter(device -> "DEV_WASH_PLAN".equals(device.getDeviceId()))
                .findFirst()
                .orElseThrow();

        deviceService.applyPlan(
                washingMachine,
                LocalDateTime.of(2026, 4, 23, 16, 30),
                LocalDateTime.of(2026, 4, 23, 9, 0)
        );

        assertEquals(LocalTime.of(16, 30), washingMachine.getSchedule());
        assertEquals(DeviceStatus.SCHEDULED, washingMachine.getStatus());
        assertEquals("16:30", repository.findById("DEV_WASH_PLAN").orElseThrow().scheduleTime());
    }

    private void setCurrentRecord(LocalDateTime timestamp, double pricePerKwh) {
        EnergyRecord record = new EnergyRecord(timestamp, timestamp.toLocalTime().toString(),
                5.0, pricePerKwh, 50, 10, 5, 50, 3.0, 1.0, 1.5, 0.5);
        simService.getCurrentEnergyRecord().set(record);
    }

    private CanonicalDeviceFixture sampleRecord(
            String deviceId,
            String displayName,
            String deviceType,
            String category,
            String powerState,
            String status
    ) {
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
                powerState,
                status,
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
