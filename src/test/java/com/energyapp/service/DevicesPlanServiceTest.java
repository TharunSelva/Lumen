package com.energyapp.service;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.DevicePlanCard;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.Dishwasher;
import com.energyapp.model.EVCharger;
import com.energyapp.model.WashingMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevicesPlanServiceTest {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private DevicesPlanService planService;

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
                stmt.setString(2, "Tharu");
                stmt.setString(3, "UTC");
                stmt.executeUpdate();
            }
        }

        planService = new DevicesPlanService();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void buildPlanCards_usesForecastSignalsForScheduledDevices() throws Exception {
        insertForecast("2026-04-23 10:30:00", 14.0, 120.0, 100.0);
        insertForecast("2026-04-23 13:00:00", 9.0, 70.0, 700.0);
        insertForecast("2026-04-23 19:00:00", 30.0, 180.0, 0.0);
        insertForecast("2026-04-24 00:30:00", 6.0, 60.0, 0.0);

        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(18, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);

        Dishwasher dishwasher = new Dishwasher("Kitchen Dishwasher");
        dishwasher.setDeviceId("DEV_DISH");
        dishwasher.setSchedule(LocalTime.of(20, 0));
        dishwasher.setStatus(DeviceStatus.SCHEDULED);

        EVCharger evCharger = new EVCharger("Driveway EV");
        evCharger.setDeviceId("DEV_EV");
        evCharger.setSchedule(LocalTime.of(23, 0));
        evCharger.setStatus(DeviceStatus.SCHEDULED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine, dishwasher, evCharger),
                LocalDateTime.of(2026, 4, 23, 9, 0)
        );

        assertEquals(3, cards.size());

        DevicePlanCard evCard = findCard(cards, "DEV_EV");
        assertEquals(LocalDateTime.of(2026, 4, 24, 0, 30), evCard.windowStart());
        assertTrue(evCard.forecastBacked());

        DevicePlanCard dishwasherCard = findCard(cards, "DEV_DISH");
        assertEquals(LocalDateTime.of(2026, 4, 23, 13, 0), dishwasherCard.windowStart());
        assertTrue(dishwasherCard.reason().contains("9.0 p/kWh"));

        DevicePlanCard washingMachineCard = findCard(cards, "DEV_WASH");
        assertEquals(LocalDateTime.of(2026, 4, 23, 13, 0), washingMachineCard.windowStart());
        assertTrue(washingMachineCard.reason().contains("9.0 p/kWh"));
        assertFalse(washingMachineCard.selectedPlan());
    }

    @Test
    void buildPlanCards_fallsBackToSavedScheduleWhenForecastDataIsUnavailable() {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(16, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine),
                LocalDateTime.of(2026, 4, 23, 9, 0)
        );

        assertEquals(1, cards.size());
        assertEquals(LocalDateTime.of(2026, 4, 23, 16, 0), cards.get(0).windowStart());
        assertFalse(cards.get(0).forecastBacked());
        assertTrue(cards.get(0).reason().contains("Forecast unavailable right now"));
        assertFalse(cards.get(0).selectedPlan());
    }

    @Test
    void buildPlanCards_omitsSkippedScheduledDevices() {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(16, 0));
        washingMachine.setSkipNextRun(true);
        washingMachine.setStatus(DeviceStatus.SKIPPED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine),
                LocalDateTime.of(2026, 4, 23, 9, 0)
        );

        assertTrue(cards.isEmpty());
    }

    @Test
    void buildPlanCards_greenEnergyOnlyPrefersGreenerWindow() throws Exception {
        insertForecast("2026-04-23 10:30:00", 6.0, 220.0, 0.0);
        insertForecast("2026-04-23 13:00:00", 35.0, 60.0, 650.0);

        WashingMachine defaultMachine = new WashingMachine("Default Laundry");
        defaultMachine.setDeviceId("DEV_DEFAULT");
        defaultMachine.setStatus(DeviceStatus.SCHEDULED);

        WashingMachine greenMachine = new WashingMachine("Green Laundry");
        greenMachine.setDeviceId("DEV_GREEN");
        greenMachine.setStatus(DeviceStatus.SCHEDULED);
        greenMachine.setGreenEnergyOnly(true);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(defaultMachine, greenMachine),
                LocalDateTime.of(2026, 4, 23, 9, 0)
        );

        assertEquals(LocalDateTime.of(2026, 4, 23, 10, 30), findCard(cards, "DEV_DEFAULT").windowStart());
        assertEquals(LocalDateTime.of(2026, 4, 23, 13, 0), findCard(cards, "DEV_GREEN").windowStart());
    }

    @Test
    void buildPlanCards_marksCardSelectedOnlyWhenAcceptedInUiSession() throws Exception {
        insertForecast("2026-04-23 13:00:00", 9.0, 70.0, 600.0);

        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(13, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine),
                LocalDateTime.of(2026, 4, 23, 9, 0),
                Map.of("DEV_WASH", LocalDateTime.of(2026, 4, 23, 13, 0))
        );

        assertTrue(cards.get(0).selectedPlan());
    }

    @Test
    void buildPlanCards_usesRuntimeForecastWindowWhenSimulationDayHasNoRows() throws Exception {
        LocalDateTime runtimeNow = LocalDateTime.now(ZoneId.of("UTC")).withSecond(0).withNano(0);
        insertForecast(runtimeNow.plusHours(2).format(TS_FMT), 7.5, 85.0, 320.0);

        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(13, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine),
                LocalDateTime.of(2024, 4, 23, 9, 0)
        );

        assertEquals(1, cards.size());
        assertTrue(cards.get(0).forecastBacked());
        assertTrue(cards.get(0).reason().contains("7.5 p/kWh"));
        assertFalse(cards.get(0).reason().contains("Forecast unavailable right now"));
    }

    @Test
    void buildPlanCards_readsIsoUtcForecastCacheRows() throws Exception {
        insertForecastIsoUtc("2026-04-24T00:00Z", 8.5, 90.0, 150.0);

        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setDeviceId("DEV_WASH");
        washingMachine.setSchedule(LocalTime.of(1, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);

        List<DevicePlanCard> cards = planService.buildPlanCards(
                List.of(washingMachine),
                LocalDateTime.of(2026, 4, 23, 23, 0)
        );

        assertEquals(1, cards.size());
        assertTrue(cards.get(0).forecastBacked());
        assertEquals(LocalDateTime.of(2026, 4, 24, 0, 0), cards.get(0).windowStart());
        assertTrue(cards.get(0).reason().contains("8.5 p/kWh"));
    }

    private DevicePlanCard findCard(List<DevicePlanCard> cards, String deviceId) {
        return cards.stream()
                .filter(card -> deviceId.equals(card.deviceId()))
                .findFirst()
                .orElseThrow();
    }

    private void insertForecast(String tsStart, double tariff, double carbon, double solar) throws Exception {
        try (Connection conn = Sqlite.openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO forecast_interval_cache(
                       home_id, ts_start, ts_end, tariff_p_per_kwh, carbon_gco2_kwh,
                       solar_shortwave_wm2, retrieved_at
                     ) VALUES(?,?,?,?,?,?,?)
                     """)) {
            LocalDateTime startTime = LocalDateTime.parse(tsStart, TS_FMT);
            stmt.setString(1, "HOME_1");
            stmt.setString(2, tsStart);
            stmt.setString(3, startTime.plusMinutes(30).format(TS_FMT));
            stmt.setDouble(4, tariff);
            stmt.setDouble(5, carbon);
            stmt.setDouble(6, solar);
            stmt.setString(7, "2026-04-23 08:00:00");
            stmt.executeUpdate();
        }
    }

    private void insertForecastIsoUtc(String tsStart, double tariff, double carbon, double solar) throws Exception {
        try (Connection conn = Sqlite.openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO forecast_interval_cache(
                       home_id, ts_start, ts_end, tariff_p_per_kwh, carbon_gco2_kwh,
                       solar_shortwave_wm2, retrieved_at
                     ) VALUES(?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, "HOME_1");
            stmt.setString(2, tsStart);
            stmt.setString(3, "2026-04-24T00:30Z");
            stmt.setDouble(4, tariff);
            stmt.setDouble(5, carbon);
            stmt.setDouble(6, solar);
            stmt.setString(7, "2026-04-23T22:00Z");
            stmt.executeUpdate();
        }
    }
}
