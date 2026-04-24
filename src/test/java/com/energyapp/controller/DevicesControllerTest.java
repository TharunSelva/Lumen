package com.energyapp.controller;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;
import com.energyapp.service.DeviceService;
import com.energyapp.service.DevicesPlanService;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevicesControllerTest {

    private String originalUserHome;
    private Path testHomeDir;
    private DeviceService deviceService;
    private SimulationService simulationService;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        testHomeDir = Files.createTempDirectory("devices-controller-test");
        System.setProperty("user.home", testHomeDir.toString());

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

        simulationService = new SimulationService();
        simulationService.getCurrentEnergyRecord().set(new EnergyRecord(
                LocalDateTime.of(2026, 4, 23, 9, 0),
                "09:00",
                2.0,
                12.5,
                120.0,
                4.0,
                1.2,
                80,
                0.0,
                0.0,
                0.0,
                0.0
        ));
        deviceService = new DeviceService(simulationService);
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void refreshDevicesPage_rendersStatusesIndicatorsAndPlanActionsWithoutBlankRegression() throws Exception {
        WashingMachine firstMachine = new WashingMachine("Laundry Washer");
        firstMachine.setSchedule(LocalTime.of(10, 30));
        firstMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(firstMachine);

        WashingMachine secondMachine = new WashingMachine("Laundry Washer");
        secondMachine.setSchedule(LocalTime.of(11, 30));
        secondMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(secondMachine);

        SmartPlug smartPlug = new SmartPlug("Office Plug");
        smartPlug.setOn(false);
        smartPlug.setStatus(DeviceStatus.OFF);
        deviceService.addDevice(smartPlug);

        DevicesController controller = createController();

        runOnFxThread(() -> {
            controller.setServicesForTest(deviceService, new DevicesPlanService(), simulationService);
            controller.refreshDevicesPage();
        });

        FlowPane deviceContainer = (FlowPane) getFieldUnchecked(controller, "deviceContainer");
        VBox planCardsContainer = (VBox) getFieldUnchecked(controller, "planCardsContainer");
        Label currentPriceLabel = (Label) getFieldUnchecked(controller, "currentPriceLabel");
        Label greenEnergyNowLabel = (Label) getFieldUnchecked(controller, "greenEnergyNowLabel");

        List<String> deviceTexts = extractTexts(deviceContainer);
        assertEquals(3, deviceContainer.getChildren().size());
        assertFalse(deviceTexts.stream().anyMatch(String::isBlank));
        assertTrue(deviceTexts.contains("Washing Machine 1"));
        assertTrue(deviceTexts.contains("Washing Machine 2"));
        assertTrue(deviceTexts.contains("Power off \u2022 SCHEDULED"));
        assertFalse(deviceTexts.contains("IDLE"));
        assertFalse(deviceTexts.contains("RUNNING"));

        List<String> planTexts = extractTexts(planCardsContainer);
        List<String> planButtons = extractButtonTexts(planCardsContainer);
        assertFalse(planCardsContainer.getChildren().isEmpty());
        assertTrue(planButtons.contains("Skip"));
        assertTrue(planButtons.contains("Open settings of device"));
        assertTrue(planButtons.contains("Use this card"));
        assertTrue(planTexts.stream().anyMatch(text -> text.contains("Washing Machine")));
        assertFalse(planTexts.stream().anyMatch(text -> text.contains("saved green schedule")));

        assertEquals("12.5 p/kWh", currentPriceLabel.getText());
        assertEquals("Greener now", greenEnergyNowLabel.getText());
    }

    @Test
    void applyingPlanCardKeepsCardVisibleAndMarksItScheduled() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setSchedule(LocalTime.of(10, 30));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(washingMachine);

        DevicesController controller = createController();

        runOnFxThread(() -> {
            controller.setServicesForTest(deviceService, new DevicesPlanService(), simulationService);
            controller.refreshDevicesPage();

            VBox planCardsContainer = (VBox) getFieldUnchecked(controller, "planCardsContainer");
            Button useCardButton = findButton(planCardsContainer, "Use this card");
            assertNotNull(useCardButton);
            useCardButton.fire();
        });

        VBox planCardsContainer = (VBox) getFieldUnchecked(controller, "planCardsContainer");
        List<String> planButtons = extractButtonTexts(planCardsContainer);
        assertTrue(planButtons.contains("Scheduled"));
    }

    @Test
    void refreshDevicesPage_updatesAfterSoftDeleteAndSkippedPlanRemoval() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setSchedule(LocalTime.of(10, 30));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(washingMachine);

        DevicesController controller = createController();

        runOnFxThread(() -> {
            controller.setServicesForTest(deviceService, new DevicesPlanService(), simulationService);
            controller.refreshDevicesPage();
            deviceService.skipNextScheduledRun(washingMachine);
            controller.refreshDevicesPage();
            deviceService.deleteDevice(washingMachine);
            controller.refreshDevicesPage();
        });

        FlowPane deviceContainer = (FlowPane) getFieldUnchecked(controller, "deviceContainer");
        VBox planCardsContainer = (VBox) getFieldUnchecked(controller, "planCardsContainer");
        Label emptyStateLabel = (Label) getFieldUnchecked(controller, "emptyStateLabel");

        assertEquals(0, deviceContainer.getChildren().size());
        assertTrue(emptyStateLabel.isVisible());
        assertTrue(extractTexts(planCardsContainer).stream().anyMatch(text -> text.contains("No plans")));
    }

    private DevicesController createController() throws Exception {
        DevicesController controller = new DevicesController();
        inject(controller, "deviceContainer", new FlowPane());
        inject(controller, "planCardsContainer", new VBox());
        inject(controller, "devicesOnSummaryLabel", new Label());
        inject(controller, "scheduledTodaySummaryLabel", new Label());
        inject(controller, "nextRunSummaryLabel", new Label());
        inject(controller, "deviceCountLabel", new Label());
        inject(controller, "currentPriceLabel", new Label());
        inject(controller, "greenEnergyNowLabel", new Label());
        inject(controller, "searchField", new TextField());
        inject(controller, "statusFilter", new ComboBox<String>());
        inject(controller, "emptyStateLabel", new Label());
        inject(controller, "btnForAdd", new Button());

        runOnFxThread(controller::initialize);
        return controller;
    }

    private List<String> extractTexts(Node node) {
        List<String> texts = new ArrayList<>();
        collectTexts(node, texts);
        return texts;
    }

    private void collectTexts(Node node, List<String> texts) {
        if (node instanceof Label label) {
            texts.add(label.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectTexts(child, texts);
            }
        }
    }

    private List<String> extractButtonTexts(Node node) {
        List<String> texts = new ArrayList<>();
        collectButtonTexts(node, texts);
        return texts;
    }

    private void collectButtonTexts(Node node, List<String> texts) {
        if (node instanceof Button button) {
            texts.add(button.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectButtonTexts(child, texts);
            }
        }
    }

    private Button findButton(Node node, String text) {
        if (node instanceof Button button && text.equals(button.getText())) {
            return button;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button match = findButton(child, text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getFieldUnchecked(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
}
