package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.ViewType;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SideBarControllerTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void sideBarShowsSimulationClockFromCurrentRecord() throws Exception {
        CountDownLatch loadLatch = new CountDownLatch(1);
        Parent[] loadedRoot = new Parent[1];
        SideBarController[] loadedController = new SideBarController[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/SideBar.fxml"));
                loadedRoot[0] = loader.load();
                loadedController[0] = loader.getController();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                loadLatch.countDown();
            }
        });

        assertTrue(loadLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(loadedRoot[0]);
        assertNotNull(loadedController[0]);

        SimulationService simulationService = new SimulationService();
        EnergyRecord record = new EnergyRecord(
                LocalDateTime.of(2026, 4, 24, 13, 30),
                "13:30",
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                0.0,
                0.0,
                0.0,
                0.0
        );

        CountDownLatch updateLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            loadedController[0].setSimulationService(simulationService);
            simulationService.getCurrentEnergyRecord().set(record);
            Platform.runLater(updateLatch::countDown);
        });

        assertTrue(updateLatch.await(2, TimeUnit.SECONDS));

        Label timeLabel = (Label) loadedRoot[0].lookup("#sidebar-sim-time");
        Label dateLabel = (Label) loadedRoot[0].lookup("#sidebar-sim-date");

        assertNotNull(timeLabel);
        assertNotNull(dateLabel);
        assertEquals("13:30", timeLabel.getText());
        assertEquals("Fri, 24 Apr", dateLabel.getText());
    }

    @Test
    void searchQueriesResolveToExpectedViews() {
        SideBarController controller = new SideBarController();

        assertEquals(ViewType.FORECASTING, controller.resolveSearchTarget("solar forecast"));
        assertEquals(ViewType.RECOMMENDATIONS, controller.resolveSearchTarget("budget tips"));
        assertEquals(ViewType.DEVICES, controller.resolveSearchTarget("appliance device"));
        assertEquals(ViewType.SETTINGS, controller.resolveSearchTarget("accessibility settings"));
        assertEquals(ViewType.SETTINGS, controller.resolveSearchTarget("faq help"));
        assertEquals(ViewType.SETTINGS, controller.resolveSearchTarget("report bug"));
    }
}
