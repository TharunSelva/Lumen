package com.energyapp.controller;

import com.energyapp.db.DbPaths;
import com.energyapp.db.DeviceRepository;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;
import com.energyapp.service.DeviceService;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceSettingsControllerTest {

    private String originalUserHome;
    private Path testHomeDir;
    private DeviceService deviceService;
    private DeviceRepository repository;

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
        testHomeDir = Files.createTempDirectory("device-settings-controller-test");
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

        repository = new DeviceRepository();
        deviceService = new DeviceService(new SimulationService());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void handleSave_updatesScheduleWhenSchedulingIsEnabled() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setStatus(DeviceStatus.OFF);
        deviceService.addDevice(washingMachine);

        DeviceSettingsController controller = createController();
        runOnFxThread(() -> {
            controller.init(deviceService, washingMachine);
            ((CheckBox) getFieldUnchecked(controller, "scheduleEnabledToggle")).setSelected(true);
            ((TextField) getFieldUnchecked(controller, "scheduleTimeField")).setText("14:45");
            controller.handleSave();
        });

        assertEquals(LocalTime.of(14, 45), washingMachine.getSchedule());
        assertEquals("14:45", repository.findById(washingMachine.getDeviceId()).orElseThrow().scheduleTime());
    }

    @Test
    void handleSave_clearsScheduleWhenSchedulingIsDisabled() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setSchedule(LocalTime.of(10, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(washingMachine);

        DeviceSettingsController controller = createController();
        runOnFxThread(() -> {
            controller.init(deviceService, washingMachine);
            ((CheckBox) getFieldUnchecked(controller, "scheduleEnabledToggle")).setSelected(false);
            controller.handleSave();
        });

        assertNull(washingMachine.getSchedule());
        assertNull(repository.findById(washingMachine.getDeviceId()).orElseThrow().scheduleTime());
    }

    @Test
    void scheduleToggle_disablesAndEnablesTimeField() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        deviceService.addDevice(washingMachine);

        DeviceSettingsController controller = createController();
        runOnFxThread(() -> controller.init(deviceService, washingMachine));

        TextField scheduleField = (TextField) getFieldUnchecked(controller, "scheduleTimeField");
        CheckBox scheduleEnabledToggle = (CheckBox) getFieldUnchecked(controller, "scheduleEnabledToggle");
        VBox scheduleRow = (VBox) getFieldUnchecked(controller, "scheduleRow");

        assertTrue(scheduleField.isDisable());
        assertEquals(180.0, scheduleField.getPrefWidth(), 0.001);
        assertTrue(scheduleField.getStyleClass().contains("devices-time-field"));
        assertTrue(scheduleRow instanceof VBox);

        runOnFxThread(() -> scheduleEnabledToggle.setSelected(true));
        assertFalse(scheduleField.isDisable());
    }

    @Test
    void handleSave_updatesSmartPlugCutoffAndPowerState() throws Exception {
        SmartPlug smartPlug = new SmartPlug("Office Plug");
        smartPlug.setPriceCutoff(18.0);
        smartPlug.setOn(false);
        smartPlug.setStatus(DeviceStatus.OFF);
        deviceService.addDevice(smartPlug);

        DeviceSettingsController controller = createController();
        runOnFxThread(() -> {
            controller.init(deviceService, smartPlug);
            ((ToggleButton) getFieldUnchecked(controller, "powerToggle")).setSelected(true);
            ((TextField) getFieldUnchecked(controller, "priceCutoffField")).setText("25.5");
            controller.handleSave();
        });

        assertTrue(smartPlug.isOn());
        assertEquals(25.5, smartPlug.getPriceCutoff(), 0.001);
        var persisted = repository.findById(smartPlug.getDeviceId()).orElseThrow();
        assertEquals("ON", persisted.powerState());
        assertEquals(25.5, persisted.priceCutoffPPerKwh(), 0.001);
    }

    @Test
    void handleSave_preservesRunningWashingMachinePowerState() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setOn(true);
        washingMachine.setStatus(DeviceStatus.RUNNING);
        washingMachine.setActiveRunUntil(LocalDateTime.now().plusMinutes(45));
        deviceService.addDevice(washingMachine);

        DeviceSettingsController controller = createController();
        runOnFxThread(() -> {
            controller.init(deviceService, washingMachine);
            ((ToggleButton) getFieldUnchecked(controller, "powerToggle")).setSelected(false);
            controller.handleSave();
        });

        assertTrue(washingMachine.isOn());
        assertTrue(((ToggleButton) getFieldUnchecked(controller, "powerToggle")).isSelected());
        assertTrue(((Label) getFieldUnchecked(controller, "scheduleHelperLabel")).getText().contains("cannot be turned off"));
    }

    @Test
    void quickActionControlsAreRemoved() {
        assertThrows(NoSuchFieldException.class, () -> DeviceSettingsController.class.getDeclaredField("useNowButton"));
        assertThrows(NoSuchFieldException.class, () -> DeviceSettingsController.class.getDeclaredField("skipButton"));
        assertThrows(NoSuchFieldException.class, () -> DeviceSettingsController.class.getDeclaredField("rescheduleButton"));
    }

    @Test
    void handleDelete_softDeletesDevice() throws Exception {
        WashingMachine washingMachine = new WashingMachine("Laundry Washer");
        washingMachine.setSchedule(LocalTime.of(10, 0));
        washingMachine.setStatus(DeviceStatus.SCHEDULED);
        deviceService.addDevice(washingMachine);

        DeviceSettingsController controller = new DeviceSettingsController() {
            @Override
            boolean confirmDelete() {
                return true;
            }
        };
        injectFormFields(controller);

        runOnFxThread(() -> {
            controller.initialize();
            controller.init(deviceService, washingMachine);
            controller.handleDelete();
        });

        assertTrue(controller.isDeleted());
        assertTrue(repository.fetchActiveByHome("HOME_1").isEmpty());
        assertFalse(repository.findById(washingMachine.getDeviceId()).orElseThrow().deletedAt().isBlank());
    }

    private DeviceSettingsController createController() throws Exception {
        DeviceSettingsController controller = new DeviceSettingsController();
        injectFormFields(controller);
        runOnFxThread(controller::initialize);
        return controller;
    }

    private void injectFormFields(DeviceSettingsController controller) throws Exception {
        inject(controller, "titleLabel", new Label());
        inject(controller, "subtitleLabel", new Label());
        inject(controller, "statusChipLabel", new Label());
        inject(controller, "currentStateLabel", new Label());
        inject(controller, "powerToggle", new ToggleButton());
        inject(controller, "greenEnergyToggle", new CheckBox());
        inject(controller, "scheduleRow", new VBox());
        inject(controller, "scheduleEnabledToggle", new CheckBox());
        inject(controller, "scheduleTimeField", new TextField());
        inject(controller, "scheduleHelperLabel", new Label());
        inject(controller, "smartPlugRow", new HBox());
        inject(controller, "priceCutoffField", new TextField());
        inject(controller, "errorLabel", new Label());
        inject(controller, "saveButton", new Button());
        inject(controller, "cancelButton", new Button());
        inject(controller, "deleteButton", new Button());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getFieldUnchecked(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
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
