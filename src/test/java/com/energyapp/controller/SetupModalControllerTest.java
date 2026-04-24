package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SetupModalControllerTest {

    @BeforeAll
    static void initJfx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @Test
    void testSaveButton_EmptyFirstNameShowsError() throws Exception {
        SetupModalController controller = new SetupModalController() {
            @Override
            protected HomeProfileRepository.HomeProfileSnapshot loadExistingProfile() {
                return null;
            }
        };

        TextField mockFirstNameField = new TextField();
        mockFirstNameField.setText("");

        TextField mockCityField = new TextField();
        mockCityField.setText("Southampton");

        Label mockErrorLabel = new Label();
        Button mockSaveButton = new Button();

        inject(controller, "firstNameTextField", mockFirstNameField);
        inject(controller, "cityTextField", mockCityField);
        inject(controller, "errorLabel", mockErrorLabel);
        inject(controller, "saveButton", mockSaveButton);

        controller.initialize();
        runOnFxThread(mockSaveButton::fire);

        assertEquals("Please enter your first name.", mockErrorLabel.getText(),
                "The UI should block submission and warn the user if the first name is missing.");
    }

    @Test
    void testSaveButton_EmptyCityShowsError() throws Exception {
        SetupModalController controller = new SetupModalController() {
            @Override
            protected HomeProfileRepository.HomeProfileSnapshot loadExistingProfile() {
                return null;
            }
        };

        TextField mockFirstNameField = new TextField();
        mockFirstNameField.setText("Alex");

        TextField mockCityField = new TextField();
        mockCityField.setText("");

        Label mockErrorLabel = new Label();
        Button mockSaveButton = new Button();

        inject(controller, "firstNameTextField", mockFirstNameField);
        inject(controller, "cityTextField", mockCityField);
        inject(controller, "errorLabel", mockErrorLabel);
        inject(controller, "saveButton", mockSaveButton);

        controller.initialize();
        runOnFxThread(mockSaveButton::fire);

        assertEquals("Please enter your city.", mockErrorLabel.getText());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
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
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
