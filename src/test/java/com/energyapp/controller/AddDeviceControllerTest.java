package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.DeviceType;
import com.energyapp.model.ScheduledDevice;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddDeviceControllerTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void addButton_blankNameShowsValidationError() throws Exception {
        AddDeviceController controller = createController();

        runOnFxThread(() -> {
            ((TextField) getFieldUnchecked(controller, "deviceName")).setText("   ");
            ((ComboBox<DeviceType>) getFieldUnchecked(controller, "selectDeviceType")).setValue(DeviceType.SMART_PLUG);
            ((Button) getFieldUnchecked(controller, "addButton")).fire();
        });

        assertEquals("Enter a device name before adding it.", ((Label) getFieldUnchecked(controller, "errorLabel")).getText());
        assertNull(controller.getNewlyCreatedDevice());
    }

    @Test
    void addButton_missingTypeShowsValidationError() throws Exception {
        AddDeviceController controller = createController();

        runOnFxThread(() -> {
            ((TextField) getFieldUnchecked(controller, "deviceName")).setText("Kitchen Plug");
            ((Button) getFieldUnchecked(controller, "addButton")).fire();
        });

        assertEquals("Choose a device type to continue.", ((Label) getFieldUnchecked(controller, "errorLabel")).getText());
        assertNull(controller.getNewlyCreatedDevice());
    }

    @Test
    void addButton_createsScheduledDeviceWithDefaults() throws Exception {
        AddDeviceController controller = createController();

        runOnFxThread(() -> {
            ((TextField) getFieldUnchecked(controller, "deviceName")).setText("Laundry Washer");
            ((ComboBox<DeviceType>) getFieldUnchecked(controller, "selectDeviceType")).setValue(DeviceType.WASHING_MACHINE);
            ((Button) getFieldUnchecked(controller, "addButton")).fire();
        });

        Device created = controller.getNewlyCreatedDevice();
        assertNotNull(created);
        assertInstanceOf(ScheduledDevice.class, created);
        ScheduledDevice scheduledDevice = (ScheduledDevice) created;
        assertEquals("Laundry Washer", scheduledDevice.getDeviceName());
        assertEquals(DeviceStatus.SCHEDULED, scheduledDevice.getStatus());
        assertEquals(LocalTime.of(10, 30), scheduledDevice.getSchedule());
        assertTrue(((Label) getFieldUnchecked(controller, "errorLabel")).getText().isBlank());
    }

    private AddDeviceController createController() throws Exception {
        AddDeviceController controller = new AddDeviceController();
        inject(controller, "addButton", new Button());
        inject(controller, "cancelButton", new Button());
        inject(controller, "selectDeviceType", new ComboBox<DeviceType>());
        inject(controller, "deviceName", new TextField());
        inject(controller, "errorLabel", new Label());

        runOnFxThread(controller::initialize);
        return controller;
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private Object getFieldUnchecked(Object target, String fieldName) {
        try {
            return getField(target, fieldName);
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
