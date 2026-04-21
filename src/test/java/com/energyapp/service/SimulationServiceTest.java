package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// CRUCIAL NOTE: this test is using Reflection to access private methods and attributes
public class SimulationServiceTest {

    private SimulationService service;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    @BeforeEach
    void setUp() {
        service = new SimulationService();
    }

    // checks SimulationService ends up with the correct cost values
    @Test
    void testAdvanceSimulation_CalculatesCostCorrectly() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        EnergyRecord record1 = new EnergyRecord(
                LocalDateTime.now(), "12:00",
                10.0, 20.0,
                0, 0, 0, 0, 0, 0, 0, 0
        );
        mockData.add(record1);

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);
        advanceMethod.invoke(service);

        waitForFxEvents();

        assertEquals(2.0, service.getTotalAccumulatedCost(), 0.001, "Cost should be £2.00");

        assertEquals(1, service.getEnergyStatsSessionHistory().size());

        assertNotNull(service.getCurrentEnergyRecord().get());
        assertEquals(10.0, service.getCurrentEnergyRecord().get().getGridImport());
    }

    // checks SimulationService fires events correctly
    @Test
    void testAdvanceSimulation_UpdatesListeners() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "Test", 5.0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        final boolean[] listenerFired = {false};
        service.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) listenerFired[0] = true;
        });

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);
        advanceMethod.invoke(service);

        waitForFxEvents();

        assertTrue(listenerFired[0], "Controllers listening to the service should be notified of updates");
        assertEquals(5.0, service.getCurrentEnergyRecord().get().getGridImport());
    }

    @Test
    void testGetCurrentEnergyRecord_NotNull() {
        assertNotNull(service.getCurrentEnergyRecord());
    }

    @Test
    void testGetEnergyStatsSessionHistory_InitiallyEmpty() {
        assertTrue(service.getEnergyStatsSessionHistory().isEmpty());
    }

    @Test
    void testGetTotalAccumulatedCost_InitiallyZero() {
        assertEquals(0.0, service.getTotalAccumulatedCost(), 0.001);
    }

    @Test
    void testGetWeeklyAccumulatedCost_InitiallyZero() {
        assertEquals(0.0, service.getWeeklyAccumulatedCost(), 0.001);
    }

    @Test
    void testMultipleSimulationAdvancements() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0));
        mockData.add(new EnergyRecord(LocalDateTime.now().plusMinutes(30), "12:30", 3.0, 15.0, 0, 0, 0, 0, 0, 0, 0, 0));
        mockData.add(new EnergyRecord(LocalDateTime.now().plusHours(1), "13:00", 7.0, 20.0, 0, 0, 0, 0, 0, 0, 0, 0));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);

        advanceMethod.invoke(service);
        waitForFxEvents();
        advanceMethod.invoke(service);
        waitForFxEvents();
        advanceMethod.invoke(service);
        waitForFxEvents();

        assertEquals(3, service.getEnergyStatsSessionHistory().size());
        // Cost: (5*10 + 3*15 + 7*20) / 100 = (50 + 45 + 140) / 100 = 2.35
        assertEquals(2.35, service.getTotalAccumulatedCost(), 0.001);
    }

    @Test
    void testSessionHistoryOrder() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 1, 12, 30);
        mockData.add(new EnergyRecord(time1, "12:00", 5.0, 10.0, 100, 5, 2, 50, 1, 1, 1, 0.5));
        mockData.add(new EnergyRecord(time2, "12:30", 3.0, 15.0, 120, 4, 1.5, 60, 0.5, 0.5, 0.5, 0.3));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);

        advanceMethod.invoke(service);
        waitForFxEvents();
        advanceMethod.invoke(service);
        waitForFxEvents();

        List<EnergyRecord> history = service.getEnergyStatsSessionHistory();
        assertEquals(2, history.size());
        assertEquals(time1, history.get(0).getTimestamp());
        assertEquals(time2, history.get(1).getTimestamp());
    }

    @Test
    void testZeroGridImportCost() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "12:00", 0.0, 25.0, 100, 10, 5, 50, 2, 1, 1, 0.5));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);
        advanceMethod.invoke(service);

        waitForFxEvents();

        assertEquals(0.0, service.getTotalAccumulatedCost(), 0.001, "Cost should be £0.00 with zero grid import");
    }

    @Test
    void testZeroPriceCost() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "12:00", 10.0, 0.0, 100, 10, 5, 50, 2, 1, 1, 0.5));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);
        advanceMethod.invoke(service);

        waitForFxEvents();

        assertEquals(0.0, service.getTotalAccumulatedCost(), 0.001, "Cost should be £0.00 with zero price");
    }

    @Test
    void testHighValueCostCalculation() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "12:00", 100.0, 50.0, 100, 10, 5, 50, 2, 1, 1, 0.5));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);
        advanceMethod.invoke(service);

        waitForFxEvents();

        // Cost = 100 * 50 / 100 = 50.0
        assertEquals(50.0, service.getTotalAccumulatedCost(), 0.001);
    }

    @Test
    void testCurrentRecordUpdatesCorrectly() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "First", 5.0, 10.0, 100, 5, 2, 50, 1, 1, 1, 0.5));
        mockData.add(new EnergyRecord(LocalDateTime.now(), "Second", 8.0, 15.0, 120, 4, 1.5, 60, 0.5, 0.5, 0.5, 0.3));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);

        advanceMethod.invoke(service);
        waitForFxEvents();
        assertEquals(5.0, service.getCurrentEnergyRecord().get().getGridImport());

        advanceMethod.invoke(service);
        waitForFxEvents();
        assertEquals(8.0, service.getCurrentEnergyRecord().get().getGridImport());
    }

    @Test
    void testAdvanceSimulation_TracksWeeklyAccumulatedCost() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        mockData.add(new EnergyRecord(LocalDateTime.now(), "12:00", 5.0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0));
        mockData.add(new EnergyRecord(LocalDateTime.now().plusMinutes(30), "12:30", 3.0, 15.0, 0, 0, 0, 0, 0, 0, 0, 0));

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);

        advanceMethod.invoke(service);
        waitForFxEvents();
        advanceMethod.invoke(service);
        waitForFxEvents();

        assertEquals(0.95, service.getWeeklyAccumulatedCost(), 0.001);
        assertEquals(0.95, service.weeklyAccumulatedCostProperty().get(), 0.001);
    }

    @Test
    void testAdvanceSimulation_ResetsWeeklyAccumulatorAtInterval336Boundary() throws Exception {
        List<EnergyRecord> mockData = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        for (int i = 0; i < 337; i++) {
            mockData.add(new EnergyRecord(
                    start.plusMinutes(i * 30L), "T" + i,
                    10.0, 10.0,
                    0, 0, 0, 0, 0, 0, 0, 0
            ));
        }

        Field dataField = SimulationService.class.getDeclaredField("simulationData");
        dataField.setAccessible(true);
        dataField.set(service, mockData);

        Method advanceMethod = SimulationService.class.getDeclaredMethod("advanceSimulation");
        advanceMethod.setAccessible(true);

        for (int i = 0; i < 336; i++) {
            advanceMethod.invoke(service);
        }
        waitForFxEvents();

        assertEquals(336.0, service.getWeeklyAccumulatedCost(), 0.001, "Interval 335 should still belong to the old week");
        assertEquals(336.0, service.getTotalAccumulatedCost(), 0.001);

        advanceMethod.invoke(service);
        waitForFxEvents();

        assertEquals(1.0, service.getWeeklyAccumulatedCost(), 0.001, "Interval 336 should start the new week");
        assertEquals(337.0, service.getTotalAccumulatedCost(), 0.001, "Total cost should still count every interval exactly once");
        assertEquals(1.0, service.weeklyAccumulatedCostProperty().get(), 0.001);
    }

    private void waitForFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
    }
}
