package com.energyapp.controller;

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
    void testSaveButton_EmptyCityShowsError() throws Exception {
        SetupModalController controller = new SetupModalController();

        // Inject Fake UI
        TextField mockTextField = new TextField();
        mockTextField.setText(""); // Simulate empty input

        Label mockErrorLabel = new Label();
        Button mockSaveButton = new Button();

        inject(controller, "cityTextField", mockTextField);
        inject(controller, "errorLabel", mockErrorLabel);
        inject(controller, "saveButton", mockSaveButton);

        // Run the initialization to hook up the event handler
        controller.initialize();

        // Simulate click
        runOnFxThread(mockSaveButton::fire);

        // Verify the business logic correctly rejected the empty location
        assertEquals("No valid city entered.", mockErrorLabel.getText(),
                "The UI should block submission and warn the user if the city is empty.");
    }

    @Test
    void testSaveButton_DefaultPlaceholderShowsError() throws Exception {
        SetupModalController controller = new SetupModalController();

        TextField mockTextField = new TextField();
        mockTextField.setText("Enter City"); // Simulate user leaving the default placeholder text

        Label mockErrorLabel = new Label();
        Button mockSaveButton = new Button();

        inject(controller, "cityTextField", mockTextField);
        inject(controller, "errorLabel", mockErrorLabel);
        inject(controller, "saveButton", mockSaveButton);

        controller.initialize();
        runOnFxThread(mockSaveButton::fire);

        assertEquals("No valid city entered.", mockErrorLabel.getText());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
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