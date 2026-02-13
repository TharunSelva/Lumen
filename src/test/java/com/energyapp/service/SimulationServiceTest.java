package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import javafx.application.Platform;
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

    private void waitForFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
    }
}